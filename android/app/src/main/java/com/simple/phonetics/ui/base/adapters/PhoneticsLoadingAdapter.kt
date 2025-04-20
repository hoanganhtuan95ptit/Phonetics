package com.simple.phonetics.ui.base.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.setBackground
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemPhoneticsLoadingBinding

@ItemAdapter
class PhoneticsLoadingAdapter(onItemClick: ((View, PhoneticsLoadingViewItem) -> Unit)? = null) : ViewItemAdapter<PhoneticsLoadingViewItem, ItemPhoneticsLoadingBinding>(onItemClick) {

    override val viewItemClass: Class<PhoneticsLoadingViewItem> by lazy {
        PhoneticsLoadingViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPhoneticsLoadingBinding {
        return ItemPhoneticsLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemPhoneticsLoadingBinding, viewType: Int, position: Int, item: PhoneticsLoadingViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.BACKGROUND)) refreshPadding(binding, item)
    }

    override fun onBindViewHolder(binding: ItemPhoneticsLoadingBinding, viewType: Int, position: Int, item: PhoneticsLoadingViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

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