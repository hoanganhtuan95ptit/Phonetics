package com.simple.phonetics.ui.adapters

import android.view.View
import androidx.core.view.updatePadding
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.ViewItemCloneable
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.setText
import com.simple.coreapp.utils.extentions.toPx
import com.simple.coreapp.utils.extentions.toText
import com.simple.phonetics.databinding.ItemTitleBinding

open class TitleAdapter(onItemClick: (View, TitleViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<TitleViewItem, ItemTitleBinding>(onItemClick) {

    override fun bind(binding: ItemTitleBinding, viewType: Int, position: Int, item: TitleViewItem) {
        super.bind(binding, viewType, position, item)

        binding.tvText.updatePadding(left = item.paddingHorizontal, right = item.paddingHorizontal)

        refreshText(binding, item)
    }

    private fun refreshText(binding: ItemTitleBinding, item: TitleViewItem) {

        binding.tvText.setText(item.text)
    }
}

class TitleViewItem(
    val id: Int,

    var text: Text<*> = emptyText(),

    var paddingHorizontal: Int = 4.toPx()
) : ViewItemCloneable {

    fun refresh() = apply {

        text = id.toText()
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )
}