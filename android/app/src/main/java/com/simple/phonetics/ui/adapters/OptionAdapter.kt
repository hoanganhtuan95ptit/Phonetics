package com.simple.phonetics.ui.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemConfigNormalBinding

open class OptionAdapter<T : OptionViewItem<*>>(onItemClick: (View, T) -> Unit = { _, _ -> }) : ViewItemAdapter<T, ItemConfigNormalBinding>(onItemClick) {

    override fun bind(binding: ItemConfigNormalBinding, viewType: Int, position: Int, item: T, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) {

            refreshText(binding, item)
        }

        if (payloads.contains(PAYLOAD_IS_SELECTED)) {

            refreshIsSelected(binding, item)
        }
    }

    override fun bind(binding: ItemConfigNormalBinding, viewType: Int, position: Int, item: T) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
        refreshIsSelected(binding, item)
    }

    private fun refreshText(binding: ItemConfigNormalBinding, item: T) {

        binding.tvText.setText(item.text)
    }

    private fun refreshIsSelected(binding: ItemConfigNormalBinding, item: T) {

        binding.tvText.setTextColor(item.textColor)
        binding.tvText.delegate.strokeColor = item.strokeColor
        binding.tvText.delegate.backgroundColor = item.backgroundColor
    }
}

open class OptionViewItem<T>(
    open val id: String,
    open val data: T,

    open var text: String = "",

    open val isSelect: Boolean,

    open val textColor: Int,
    open val strokeColor: Int,
    open val backgroundColor: Int
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,
        textColor to PAYLOAD_IS_SELECTED,
        backgroundColor to PAYLOAD_IS_SELECTED
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_SELECTED = "PAYLOAD_IS_SELECTED"