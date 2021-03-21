package com.rakiwow.fitsynckoala.presentation.preview

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.rakiwow.fitsynckoala.BuildConfig
import com.rakiwow.fitsynckoala.R
import com.rakiwow.fitsynckoala.databinding.FragmentPreviewBinding
import com.rakiwow.fitsynckoala.model.FitResult
import com.rakiwow.fitsynckoala.model.FitTime
import com.rakiwow.fitsynckoala.model.RefreshTokenResult
import com.rakiwow.fitsynckoala.util.DataState
import com.rakiwow.fitsynckoala.util.FitAnalyzer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@ExperimentalTime
@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private val previewViewModel: PreviewViewModel by viewModels()
    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val STRAVA_CLIENT_ID = BuildConfig.STRAVA_CLIENT_ID
        private const val STRAVA_CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET
    }

    private lateinit var fitnessAnalyzer: FitAnalyzer
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    private val fitResult = FitResult(FitTime(), 0f, 0f)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fitnessAnalyzer = FitAnalyzer(requireContext())

        if (allPermissionsGranted()) {
            fitnessAnalyzer.onTextRecognitionSuccess = { textTime, textKcal, textDistance ->
                textTime?.let { time ->
                    binding.tvTotalTimeValue.text = time.getFormattedText()
                    fitResult.time = time
                }
                textKcal?.let { kcal ->
                    binding.tvTotalCaloriesValue.text = "$kcal kcal"
                    fitResult.kcal = kcal
                }
                textDistance?.let { distance ->
                    binding.tvTotalDistanceValue.text = "$distance km"
                    fitResult.distance = (distance * 1000)
                }
            }
            startCamera()
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setup()
        observeViewModel()
    }

    private fun setup() {
        binding.btnUploadData.setOnClickListener {
            prepareFitResult()
        }

        binding.btnManuallyUploadData.setOnClickListener {
            cameraProvider.unbindAll()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to manually set your activity?")
                .setPositiveButton("Set manually") { _, _ ->
                    cameraExecutor.shutdown()
                    handleUploadByExpiration(previewViewModel.expiresAtFlow, 0f, 0, 0f)
                }
                .setNegativeButton("Dismiss") { _, _ ->
                    bindUseCase()
                }
                .setOnCancelListener {
                    bindUseCase()
                }
                .create().show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun prepareFitResult() {
        cameraProvider.unbindAll()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.are_results_correct_prompt))
            .setMessage(
                getString(R.string.time) + " ${fitResult.time.getFormattedText()}\n" +
                        getString(R.string.kcal_burned) + " ${fitResult.kcal}\n" +
                        getString(R.string.distance) + " ${fitResult.distance / 1000} km"
            )
            .setPositiveButton(getString(R.string.correct)) { _, _ ->
                cameraExecutor.shutdown()
                handleUploadByExpiration(
                    previewViewModel.expiresAtFlow,
                    fitResult.distance,
                    fitResult.time.getTotalSeconds(),
                    fitResult.kcal
                )
            }
            .setNegativeButton(getString(R.string.no)) { _, _ ->
            }
            .setOnDismissListener {
                bindUseCase()
            }
            .create().show()
    }

    private fun handleUploadByExpiration(
        expiresAtFlow: LiveData<Int>,
        distance: Float,
        time: Int,
        kcal: Float
    ) {
        with(expiresAtFlow) {
            val expiresAt = value
            if (expiresAt == null) {
                Timber.d("Access token expiration is null - Navigating to login fragment to get new access token")
                findNavController().navigate(
                    PreviewFragmentDirections.actionPreviewFragmentToLoginFragment(
                        distance,
                        time,
                        kcal
                    )
                )
            }
            expiresAt?.let {
                when {
                    expiresAt == 0 -> {
                        Timber.d("Access token expiration is 0 - Navigating to login fragment to get new access token")
                        findNavController().navigate(
                            PreviewFragmentDirections.actionPreviewFragmentToLoginFragment(
                                distance,
                                time,
                                kcal
                            )
                        )
                    }
                    expiresAt > System.currentTimeMillis() / 1000L -> {
                        Timber.d("Access token is valid - Navigating to sync fragment")
                        findNavController().navigate(
                            PreviewFragmentDirections.actionPreviewFragmentToSyncFragment(
                                distance,
                                time,
                                kcal
                            )
                        )
                    }
                    expiresAt <= System.currentTimeMillis() / 1000L -> {
                        Timber.d("Access token is too old. Fetching new access token with refresh token")
                        previewViewModel.fetchRefreshTokenResult(
                            STRAVA_CLIENT_ID.toInt(),
                            STRAVA_CLIENT_SECRET,
                            previewViewModel.refreshTokenFlow.value.toString()
                        )
                    }
                    else -> {
                        Timber.d("Access token expiration is $expiresAt - This is an else escape.")
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder()
                .build()

            bindUseCase()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCase() {
        try {
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, fitnessAnalyzer)
                }

            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
            )

        } catch (exc: Exception) {
            Timber.e("Use case binding failed")
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeViewModel() {

        previewViewModel.refreshTokenResult.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandled()?.let { dataState ->
                when (dataState) {
                    is DataState.Success<RefreshTokenResult> -> {
                        binding.pbPreview.visibility = View.GONE
                        binding.vProgressFullBackground.visibility = View.GONE
                        with(dataState.data) {
                            previewViewModel.persistInstance(
                                accessToken = accessToken,
                                expiresAt = expiresAt,
                                expiresIn = expiresIn,
                                refreshToken = refreshToken
                            )
                        }
                        findNavController().navigate(
                            PreviewFragmentDirections.actionPreviewFragmentToSyncFragment(
                                fitResult.distance,
                                fitResult.time.getTotalSeconds(),
                                fitResult.kcal
                            )
                        )
                    }
                    is DataState.Loading -> {
                        binding.pbPreview.visibility = View.VISIBLE
                        binding.vProgressFullBackground.visibility = View.VISIBLE
                    }
                    is DataState.Error -> {
                        bindUseCase()
                        binding.pbPreview.visibility = View.GONE
                        binding.vProgressFullBackground.visibility = View.GONE
                        Timber.d("Problem retrieving new access token by refresh token")
                        Timber.d(dataState.exception)
                        Toast.makeText(
                            context,
                            getString(R.string.network_error_try_again),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })

        // Observe datastore data, or else values are null
        previewViewModel.expiresAtFlow.observe(viewLifecycleOwner, {})
        previewViewModel.refreshTokenFlow.observe(viewLifecycleOwner, {})
    }
}