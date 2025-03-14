package com.simple.phonetics.ui.phonetic.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemHistoryBinding

class HistoryAdapter(onItemClick: (View, HistoryViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<HistoryViewItem, ItemHistoryBinding>(onItemClick) {

    override fun bind(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.TEXT)) refreshText(binding, item)
    }

    override fun bind(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
    }

    private fun refreshText(binding: ItemHistoryBinding, item: HistoryViewItem) {
        binding.tvText.text = item.text
    }
}

data class HistoryViewItem(
    val id: String,

    val text: CharSequence = "",
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
    )
}