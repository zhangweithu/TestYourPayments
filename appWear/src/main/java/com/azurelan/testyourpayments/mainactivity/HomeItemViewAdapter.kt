package com.azurelan.testyourpayments.mainactivity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.azurelan.testyourpayments.R


class HomeItemViewAdapter(
    private val viewDataList: List<HomeItemViewData>,
    private val onClick: (HomeItemViewData) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return viewDataList[position].viewType
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(
                when (viewType) {
                    VIEW_TYPE_HOME_TITLE_ROW -> R.layout.home_item_title
                    else -> R.layout.home_item_button
                },
                viewGroup,
                /* attachToRoot= */ false)

        return HomeItemViewHolder(view, onClick)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val homeItemViewData = viewDataList[position]
        (viewHolder as HomeItemViewHolder).bind(homeItemViewData)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = viewDataList.size

    companion object {
        const val VIEW_TYPE_HOME_TITLE_ROW = 1
        const val VIEW_TYPE_HOME_GARDENER_ITEM_ROW = 2
        const val VIEW_TYPE_HOME_TREE_ITEM_ROW = 3
        const val VIEW_TYPE_HOME_ROSE_ITEM_ROW = 4
        const val VIEW_TYPE_HOME_SERVICE_ITEM_ROW = 5
        const val VIEW_TYPE_HOME_LOG_PAGE_ROW = 6
        const val VIEW_TYPE_HOME_CONTACT_ROW = 7
    }
}