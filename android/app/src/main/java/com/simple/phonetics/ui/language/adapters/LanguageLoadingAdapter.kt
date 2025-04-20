package com.simple.phonetics.ui.language.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.phonetics.databinding.ItemLanguageLoadingBinding
import java.util.UUID

@ItemAdapter
class LanguageLoadingAdapter() : ViewItemAdapter<LanguageLoadingViewItem, ItemLanguageLoadingBinding>() {

    override val viewItemClass: Class<LanguageLoadingViewItem> by lazy {
        LanguageLoadingViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemLanguageLoadingBinding {
        return ItemLanguageLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemLanguageLoadingBinding, viewType: Int, position: Int, item: LanguageLoadingViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.delegate.setBackground(item.background)

        binding.ivFlag.delegate.backgroundColor = item.loadingColor
        binding.tvName.delegate.backgroundColor = item.loadingColor
    }
}

class LanguageLoadingViewItem(
    var loadingColor: Int,
    var background: Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        UUID.randomUUID().toString()
    )
}