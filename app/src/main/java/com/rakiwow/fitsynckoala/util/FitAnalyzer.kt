package com.rakiwow.fitsynckoala.util

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.rakiwow.fitsynckoala.model.FitTime
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import java.util.concurrent.ArrayBlockingQueue

class FitAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    var onTextRecognitionSuccess: ((time: FitTime?, kcal: Float?, distance: Float?) -> Unit)? = null
    private val kcalValuesQueue: ArrayBlockingQueue<Float> = ArrayBlockingQueue(20)
    private val distanceValuesQueue: ArrayBlockingQueue<Float> = ArrayBlockingQueue(20)
    private val timeValuesQueue: ArrayBlockingQueue<FitTime> = ArrayBlockingQueue(20)

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient()
            recognizer.process(image)
                .addOnSuccessListener {
                    printText(it)
                }
                .addOnFailureListener {
                    Toast.makeText(context, it.toString(), Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()
                }
        }
    }

    private fun printText(result: Text) {
        var mostOccurredFitTime: FitTime? = null
        var mostOccurredKcal: Float? = null
        var mostOccurredDistance: Float? = null

        for (i in 0 until result.textBlocks.size) {
            val blockText = result.textBlocks[i].text
            if (blockText.contains("hr") && blockText.contains("min") && blockText.contains("sec")) {
                val textHrMinSec = blockText.split("hr", "min")
                if (textHrMinSec.size == 3) {
                    val fitTime = FitTime(
                        getIntFromString(textHrMinSec[0]),
                        getIntFromString(textHrMinSec[1]),
                        getIntFromString(textHrMinSec[2])
                    )

                    if (!timeValuesQueue.offer(fitTime)) {
                        timeValuesQueue.take()
                        timeValuesQueue.offer(fitTime)
                    }
                    mostOccurredFitTime = getMostOccurredFitTime(timeValuesQueue)
                }
            } else if (blockText.contains("min") && blockText.contains("sec")) {
                val textHrMinSec = blockText.split("min")
                if (textHrMinSec.size == 2) {
                    val fitTime = FitTime(
                        0,
                        getIntFromString(textHrMinSec[0]),
                        getIntFromString(textHrMinSec[1])
                    )

                    if (!timeValuesQueue.offer(fitTime)) {
                        timeValuesQueue.take()
                        timeValuesQueue.offer(fitTime)
                    }
                    mostOccurredFitTime = getMostOccurredFitTime(timeValuesQueue)
                }
            } else if (blockText.contains("sec")) {
                val fitTime = FitTime(
                    0,
                    0,
                    getIntFromString(blockText)
                )

                if (!timeValuesQueue.offer(fitTime)) {
                    timeValuesQueue.take()
                    timeValuesQueue.offer(fitTime)
                }
                mostOccurredFitTime = getMostOccurredFitTime(timeValuesQueue)
            } else if (blockText.contains("kcal")) {
                val blockTextFloat = replaceSimilarCharacters(blockText)
                val numbersIndexStart = blockTextFloat.indexOfFirst { it.isDigit() }
                if (numbersIndexStart >= 0) {

                    val numbersIndexEnd = blockTextFloat.substring(numbersIndexStart, blockTextFloat.length)
                        .indexOfFirst { !it.isDigit() && it != '.' }

                    val numbers =
                        blockTextFloat.substring(numbersIndexStart, numbersIndexStart + numbersIndexEnd)
                            .toFloat()

                    if (!kcalValuesQueue.offer(numbers)) {
                        kcalValuesQueue.take()
                        kcalValuesQueue.offer(numbers)
                    }
                    mostOccurredKcal = getMostOccurredElement(kcalValuesQueue)?.toFloat()
                }
            } else if (blockText.contains("km")) {
                val blockTextFloat = replaceSimilarCharacters(blockText)
                val numbersIndexStart = blockTextFloat.indexOfFirst { it.isDigit() }
                if (numbersIndexStart >= 0) {

                    val numbersIndexEnd = blockTextFloat.substring(numbersIndexStart, blockTextFloat.length)
                        .indexOfFirst { !it.isDigit() && it != '.' }
                    val numbers =
                        blockTextFloat.substring(
                            numbersIndexStart,
                            numbersIndexStart + numbersIndexEnd
                        ).toFloat()

                    if (distanceValuesQueue.offer(numbers)) {
                        distanceValuesQueue.take()
                        distanceValuesQueue.offer(numbers)
                    }
                    mostOccurredDistance = getMostOccurredElement(distanceValuesQueue)?.toFloat()
                }
            }
        }
        onTextRecognitionSuccess?.invoke(
            mostOccurredFitTime,
            mostOccurredKcal,
            mostOccurredDistance
        )
    }

    private fun getIntFromString(text: String): Int {
        text.replace("I", "1").replace("]", "1").replace("O", "0")
        return if (text.none { it.isDigit() }) {
            0
        } else {
            text.filter { it.isDigit() }.toInt()
        }
    }

    private fun replaceSimilarCharacters(text: String): String {
        return text.replace("I", "1").replace("]", "1").replace("O", "0").replace(" ", "")
    }

    private fun getMostOccurredFitTime(fitTime: ArrayBlockingQueue<FitTime>): FitTime? {
        val fitTimeFrequencies = fitTime.groupingBy { it }.eachCount()
        return fitTimeFrequencies.maxByOrNull { it.value }?.key
    }

    private fun getMostOccurredElement(valueList: ArrayBlockingQueue<Float>): Number? {
        val valueFrequencies = valueList.groupingBy { it }.eachCount()
        return valueFrequencies.maxByOrNull { it.value }?.key
    }
}