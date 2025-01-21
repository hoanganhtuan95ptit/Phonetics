package com.simple.phonetics.ui.phonetics.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.databinding.ItemPhoneticsHistoryBinding

class HistoryAdapter(onItemClick: (View, HistoryViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<HistoryViewItem, ItemPhoneticsHistoryBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) {

            refreshText(binding, item)
        }

        if (payloads.contains(PAYLOAD_IS_LAST)) {

            refreshDivider(binding, item)
        }

        if (payloads.contains(PAYLOAD_DIVIDER_COLOR)) {

            refreshDividerColor(binding, item)
        }
    }

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
        refreshDivider(binding, item)
        refreshDividerColor(binding, item)
    }

    private fun refreshText(binding: ItemPhoneticsHistoryBinding, item: HistoryViewItem) {

        binding.tvText.text = item.text
    }

    private fun refreshDivider(binding: ItemPhoneticsHistoryBinding, item: HistoryViewItem) {

        binding.vDivider.setVisible(item.dividerShow)
    }

    private fun refreshDividerColor(binding: ItemPhoneticsHistoryBinding, item: HistoryViewItem) {

        binding.vDivider.setColors(item.dividerColor)
    }
}

data class HistoryViewItem(
    val id: String,

    val text: CharSequence = "",

    val dividerColor: Int,
    val dividerShow: Boolean = false,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,
        dividerShow to PAYLOAD_IS_LAST,
        dividerColor to PAYLOAD_DIVIDER_COLOR
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_LAST = "PAYLOAD_IS_LAST"
private const val PAYLOAD_DIVIDER_COLOR = "PAYLOAD_DIVIDER_COLOR"
