package com.azurelan.testyourpayments.externalvisiblelog

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.azurelan.testyourpayments.R

class LogViewHolder(view: View)
    : RecyclerView.ViewHolder(view) {
    private val context = view.context
    private val logTextView: TextView = view.findViewById(R.id.log)

    fun bind(logViewData: LogViewData) {
        logTextView.text = logViewData.text
    }
}