package com.simple.phonetics.ui.base.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.setBackground
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemPhoneticsLoadingBinding

class PhoneticsLoadingAdapter(onItemClick: ((View, PhoneticsLoadingViewItem) -> Unit)? = null) : ViewItemAdapter<PhoneticsLoadingViewItem, ItemPhoneticsLoadingBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsLoadingBinding, viewType: Int, position: Int, item: PhoneticsLoadingViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.BACKGROUND)) refreshPadding(binding, item)
    }

    override fun bind(binding: ItemPhoneticsLoadingBinding, viewType: Int, position: Int, item: PhoneticsLoadingViewItem) {
        super.bind(binding, viewType, position, item)

        refreshPadding(binding, item)
    }

    private fun refreshPadding(binding: ItemPhoneticsLoadingBinding, item: PhoneticsLoadingViewItem) {
        binding.tvIpa.delegate.setBackground(item.background)
        binding.tvText.delegate.setBackground(item.background)
    }
}

data class PhoneticsLoadingViewItem(
    val id: String,
    val background: Background = DEFAULT_BACKGROUND
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        background to Payload.BACKGROUND
    )
}