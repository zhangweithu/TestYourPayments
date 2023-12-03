package com.azurelan.testyourpayments.shared.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.azurelan.testyourpayments.shared.R

object ContactUtils {

    enum class FormFactor {
        PHONE,
        TV,
        WEAR,
    }
    fun contactByEmail(context: Context, versionCode: Int, formFactor: FormFactor) {
        val intent =
            Intent(
                Intent.ACTION_SENDTO,
                // only email apps should handle this
                Uri.parse(
                    context.getString(
                        R.string.report_an_issue_email_template,
                        versionCode,
                        String.format("Device Type: %s\n\n", formFactor.name))))
        Handler(Looper.getMainLooper()).post {
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.report_an_issue_cannot_invoke_email),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }
}