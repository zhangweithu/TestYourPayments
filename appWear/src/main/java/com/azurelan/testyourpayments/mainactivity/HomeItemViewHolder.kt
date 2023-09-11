package com.azurelan.testyourpayments.mainactivity

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.azurelan.testyourpayments.R
import com.azurelan.testyourpayments.mainactivity.HomeItemViewAdapter.Companion.VIEW_TYPE_HOME_TITLE_ROW
import com.azurelan.testyourpayments.shared.utils.AzureLanLog

class HomeItemViewHolder(view: View, val onClick: (HomeItemViewData) -> Unit)
    : RecyclerView.ViewHolder(view) {
    private val currentView = view
    private var currentViewData: HomeItemViewData? = null

    init {
        // Define click listener for the ViewHolder's View.
        currentView.setOnClickListener {
            currentViewData?.let {
                onClick(it)
            }
        }
    }

    fun bind(homeItemViewData: HomeItemViewData) {
        currentViewData = homeItemViewData
        when (homeItemViewData.viewType) {
            VIEW_TYPE_HOME_TITLE_ROW -> {
                val titleView: TextView = currentView.findViewById(R.id.title)
                titleView.text = homeItemViewData.text
            }
            else -> {
                val buttonView: Button = currentView.findViewById(R.id.button)
                buttonView.text = homeItemViewData.text
            }
        }
    }
}