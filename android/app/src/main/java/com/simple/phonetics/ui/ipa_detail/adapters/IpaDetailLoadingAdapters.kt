package com.simple.phonetics.ui.ipa_detail.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Payload
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemIpaDetailBinding
import com.simple.phonetics.databinding.ItemIpaDetailLoadingBinding
import com.simple.phonetics.entities.Ipa
import com.tuanha.adapter.annotation.AdapterPreview

@AdapterPreview
class IpaDetailLoadingAdapters(onItemClick: (View, IpaDetailLoadingViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<IpaDetailLoadingViewItem, ItemIpaDetailLoadingBinding>(onItemClick) {

    override fun bind(binding: ItemIpaDetailLoadingBinding, viewType: Int, position: Int, item: IpaDetailLoadingViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.BACKGROUND)) refreshBackground(binding, item)
    }

    override fun bind(binding: ItemIpaDetailLoadingBinding, viewType: Int, position: Int, item: IpaDetailLoadingViewItem) {
        super.bind(binding, viewType, position, item)

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