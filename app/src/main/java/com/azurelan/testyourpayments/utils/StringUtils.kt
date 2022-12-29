package com.azurelan.testyourpayments.utils

import android.icu.util.Calendar

object StringUtils {

    fun getCurrentTimeDisplay(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%d.%d.%d %d:%d:%d:%d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            calendar.get(Calendar.MILLISECOND),
        )
    }
}