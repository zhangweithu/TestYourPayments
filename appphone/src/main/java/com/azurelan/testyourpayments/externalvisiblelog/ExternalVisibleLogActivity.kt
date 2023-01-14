package com.azurelan.testyourpayments.externalvisiblelog

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog

class ExternalVisibleLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_visible_log)

        val logView: TextView = findViewById(R.id.log)
        logView.text = ExternallyVisibleLog.getLog()
    }
}