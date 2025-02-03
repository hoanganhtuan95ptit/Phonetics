package com.simple.phonetics.ui.phonetics.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemPhoneticsHistoryBinding

class HistoryAdapter(onItemClick: (View, HistoryViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<HistoryViewItem, ItemPhoneticsHistoryBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
    }

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
    }

    private fun refreshText(binding: ItemPhoneticsHistoryBinding, item: HistoryViewItem) {

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
        text to PAYLOAD_TEXT,
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"