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

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_clear_24)

        val logView: TextView = findViewById(R.id.log)
        logView.text = ExternallyVisibleLog.getLog()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // close this activity as oppose to navigating up
        return false
    }
}