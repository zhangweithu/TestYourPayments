package com.azurelan.testyourpayments.shared.externalvisiblelog

import com.azurelan.testyourpayments.shared.utils.StringUtils

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