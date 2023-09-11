package com.azurelan.testyourpayments.externalvisiblelog

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.databinding.ActivityExternalVisibleLogBinding
import com.azurelan.testyourpayments.scrollingutils.ScrollingLayoutCallback
import com.azurelan.testyourpayments.shared.externalvisiblelog.ExternallyVisibleLog

class ExternalVisibleLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExternalVisibleLogBinding
    private lateinit var recyclerView: WearableRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExternalVisibleLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.apply {
            // To align the edge children (first and last) with the center of the screen.
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(
                this@ExternalVisibleLogActivity, ScrollingLayoutCallback()
            )
            adapter = LogViewAdapter(
                createLogViewDataList(ExternallyVisibleLog.getLog()))
        }
        recyclerView.adapter?.notifyDataSetChanged()
        recyclerView.requestFocus()
    }

    private fun createLogViewDataList(log: String): List<LogViewData> {
        val logItemList = log.split("\n\n")
        return logItemList.map { it ->
            LogViewData(LogViewAdapter.VIEW_TYPE_LOG_TEXT_ROW, it) }
    }
}