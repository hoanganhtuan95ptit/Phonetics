package com.simple.phonetics.ui.adapters

import android.view.View
import androidx.annotation.CallSuper
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.ViewItemCloneable
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.setText
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

        binding.tvText.isSelected = item.isSelect
    }
}

open class OptionViewItem<T>(
    open val id: String,
    open val data: T,

    open var text: Text<*> = emptyText(),
    open var isSelect: Boolean = false
) : ViewItemCloneable {

    @CallSuper
    open fun refresh(isSelected: Boolean) = apply {

        this.isSelect = isSelected
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,
        isSelect to PAYLOAD_IS_SELECTED
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_SELECTED = "PAYLOAD_IS_SELECTED"