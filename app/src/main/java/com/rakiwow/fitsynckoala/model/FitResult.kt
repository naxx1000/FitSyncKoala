package com.rakiwow.fitsynckoala.model

data class FitResult(
    var time: FitTime = FitTime(),
    var kcal: Float = 0f,
    var distance: Float = 0f
)