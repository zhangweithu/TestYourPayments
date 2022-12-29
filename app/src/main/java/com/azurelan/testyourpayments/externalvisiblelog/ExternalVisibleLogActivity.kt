package com.azurelan.testyourpayments.externalvisiblelog

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.databinding.ActivityExternalVisibleLogBinding

class ExternalVisibleLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExternalVisibleLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExternalVisibleLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val logView: TextView = findViewById(R.id.log)
        logView.text = ExternallyVisibleLog.getLog()
    }
}