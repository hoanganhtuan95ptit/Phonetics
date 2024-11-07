package com.simple.phonetics.ui.adapters

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.core.view.updatePadding
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.databinding.ItemTitleBinding

open class TitleAdapter(onItemClick: (View, TitleViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<TitleViewItem, ItemTitleBinding>(onItemClick) {

    override fun bind(binding: ItemTitleBinding, viewType: Int, position: Int, item: TitleViewItem) {
        super.bind(binding, viewType, position, item)

        binding.tvText.setTextColor(item.textColor)
        binding.tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.textSize)
        binding.tvText.updatePadding(left = item.paddingHorizontal, right = item.paddingHorizontal)

        refreshText(binding, item)
    }

    private fun refreshText(binding: ItemTitleBinding, item: TitleViewItem) {

        binding.tvText.setText(item.text)
    }
}

class TitleViewItem(
    var text: String = "",
    var textSize: Float = 16f,
    var textColor: Int = Color.BLACK,
    var paddingHorizontal: Int = DP.DP_4
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        text
    )
}