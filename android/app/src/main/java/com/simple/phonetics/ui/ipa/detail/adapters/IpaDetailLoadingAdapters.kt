package com.simple.phonetics.ui.ipa.detail.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemIpaDetailLoadingBinding
import com.tuanha.adapter.ViewItemAdapter
import com.tuanha.adapter.annotation.ItemAdapter
import com.tuanha.adapter.entities.ViewItem

@ItemAdapter
class IpaDetailLoadingAdapters(onItemClick: (View, IpaDetailLoadingViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<IpaDetailLoadingViewItem, ItemIpaDetailLoadingBinding>(onItemClick) {

    override val viewItemClass: Class<IpaDetailLoadingViewItem> by lazy {
        IpaDetailLoadingViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemIpaDetailLoadingBinding {
        return ItemIpaDetailLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemIpaDetailLoadingBinding, viewType: Int, position: Int, item: IpaDetailLoadingViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.BACKGROUND)) refreshBackground(binding, item)
    }

    override fun onBindViewHolder(binding: ItemIpaDetailLoadingBinding, viewType: Int, position: Int, item: IpaDetailLoadingViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshBackground(binding, item)
    }

    private fun refreshBackground(binding: ItemIpaDetailLoadingBinding, item: IpaDetailLoadingViewItem) {
        binding.frameContent.delegate.setBackground(item.background)
    }
}

data class IpaDetailLoadingViewItem(
    val id: String,

    val background: Background
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        background to Payload.BACKGROUND,
    )
}