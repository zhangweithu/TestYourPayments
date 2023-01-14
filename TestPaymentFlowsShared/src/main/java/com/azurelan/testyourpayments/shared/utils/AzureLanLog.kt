package com.azurelan.testyourpayments.shared.utils

import android.text.Html
import android.util.Log

class AzureLanLog {
    companion object {
        private const val TAG = "TestPaymentFlows"
        fun i(message: String) {
            Log.i(TAG, message)
        }

        fun d(message: String) {
            Log.d(TAG, message)
        }

        fun e(message: String) {
            Log.e(TAG, message)
        }

        fun v(message: String) {
            Log.v(TAG, message)
        }

        fun wtf(message: String) {
            Log.wtf(TAG, message)
        }

        fun i(template: String, vararg args: Any) {
            Log.i(TAG, String.format(template, *args))
        }

        fun d(template: String, vararg args: Any) {
            Log.d(TAG, String.format(template, *args))
        }

        fun e(template: String, vararg args: Any) {
            Log.e(TAG, String.format(template, *args))
        }

        fun v(template: String, vararg args: Any) {
            Log.v(TAG, String.format(template, *args))
        }

        fun wtf(template: String, vararg args: Any) {
            Log.wtf(TAG, String.format(template, *args))
        }

        fun createWrappedMessageForList(list: List<Any>): String {
            val stringBuilder = StringBuilder()
            for (i in list.indices) {
                stringBuilder.append(list[i].toString())
                stringBuilder.append(Html.fromHtml("<br>"))
            }
            return stringBuilder.toString()
        }
    }
}