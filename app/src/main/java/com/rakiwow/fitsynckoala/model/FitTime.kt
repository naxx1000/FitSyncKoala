package com.rakiwow.fitsynckoala.model

data class FitTime(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
) {
    fun getFormattedText(): String {
        if (hour == 0) {
            if (minute == 0) {
                return "$second sec"
            }
            return "$minute min, $second sec"
        }
        return "$hour hr, $minute min, $second sec"
    }

    fun getTotalSeconds() = (hour * 60 * 60) + (minute * 60) + second
}