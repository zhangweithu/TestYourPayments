package com.azurelan.testyourpayments.externalvisiblelog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.azurelan.testyourpayments.R


class LogViewAdapter(private val viewDataList: List<LogViewData>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return viewDataList[position].viewType
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(
                R.layout.log_item,
                viewGroup,
                /* attachToRoot= */ false)

        return LogViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val logViewData = viewDataList[position]
        (viewHolder as LogViewHolder).bind(logViewData)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = viewDataList.size

    companion object {
        const val VIEW_TYPE_LOG_TEXT_ROW = 1
    }
}