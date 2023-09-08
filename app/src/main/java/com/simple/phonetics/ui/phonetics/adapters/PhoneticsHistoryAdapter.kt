package com.simple.phonetics.ui.phonetics.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.ViewItemCloneable
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.setText
import com.simple.coreapp.utils.extentions.setVisible
import com.simple.coreapp.utils.extentions.toText
import com.simple.phonetics.databinding.ItemPhoneticsHistoryBinding

class PhoneticsHistoryAdapter(onItemClick: (View, PhoneticsHistoryViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<PhoneticsHistoryViewItem, ItemPhoneticsHistoryBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: PhoneticsHistoryViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) {

            refreshText(binding, item)
        }

        if (payloads.contains(PAYLOAD_IS_LAST)) {

            refreshDivider(binding, item)
        }
    }

    override fun bind(binding: ItemPhoneticsHistoryBinding, viewType: Int, position: Int, item: PhoneticsHistoryViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
        refreshDivider(binding, item)
    }

    private fun refreshText(binding: ItemPhoneticsHistoryBinding, item: PhoneticsHistoryViewItem) {

        binding.tvText.setText(item.text)
    }

    private fun refreshDivider(binding: ItemPhoneticsHistoryBinding, item: PhoneticsHistoryViewItem) {

        binding.vDivider.setVisible(!item.isLast)
    }
}

data class PhoneticsHistoryViewItem(
    val id: String,

    var text: Text<*> = emptyText(),
    var isLast: Boolean = false,
) : ViewItemCloneable {

    fun refresh(b: Boolean) = apply {

        text = id.toText()

        isLast = b
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,
        isLast to PAYLOAD_IS_LAST
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_LAST = "PAYLOAD_IS_LAST"
