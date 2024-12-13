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

    override fun bind(binding: ItemTitleBinding, viewType: Int, position: Int, item: TitleViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(TEXT_COLOR)) {

            refreshTextColor(binding, item)
        }
    }

    override fun bind(binding: ItemTitleBinding, viewType: Int, position: Int, item: TitleViewItem) {
        super.bind(binding, viewType, position, item)

        binding.tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.textSize)
        binding.tvText.updatePadding(left = item.paddingHorizontal, right = item.paddingHorizontal)

        refreshText(binding, item)
        refreshTextColor(binding, item)
    }

    private fun refreshText(binding: ItemTitleBinding, item: TitleViewItem) {

        binding.tvText.setText(item.text)
    }

    private fun refreshTextColor(binding: ItemTitleBinding, item: TitleViewItem) {

        binding.tvText.setTextColor(item.textColor)
    }
}

class TitleViewItem(
    var text: CharSequence = "",
    var textSize: Float = 16f,
    var textColor: Int = Color.BLACK,
    var paddingHorizontal: Int = DP.DP_4
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        text
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        textColor to TEXT_COLOR
    )
}

private const val TEXT_COLOR = "TEXT_COLOR"