package com.azurelan.testyourpayments.externalvisiblelog

import com.azurelan.testyourpayments.utils.StringUtils

object ExternallyVisibleLog {
    private var logs = "Logs\n\n"

    fun appendNewLog(msg: String) {
        logs += String.format(
            "%s %s\n\n",
            StringUtils.getCurrentTimeDisplay(),
            msg,
        )
    }

    fun getLog():String {
        return logs.toString()
    }
}