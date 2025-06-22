package com.simple.phonetics.ui.language.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.setText
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemLanguageBinding
import com.simple.phonetics.entities.Language

@ItemAdapter
class LanguageAdapter(onItemClick: (View, LanguageViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<LanguageViewItem, ItemLanguageBinding>(onItemClick) {

    override val viewItemClass: Class<LanguageViewItem> by lazy {
        LanguageViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemLanguageBinding {
        return ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.NAME)) refreshName(binding, item)
        if (payloads.contains(Payload.THEME)) refreshTheme(binding, item)
    }

    override fun onBindViewHolder(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.ivFlag.setImage(item.image, CircleCrop())

        refreshName(binding, item)
        refreshTheme(binding, item)
    }

    private fun refreshName(binding: ItemLanguageBinding, item: LanguageViewItem) {

        binding.tvName.setText(item.name)
    }

    private fun refreshTheme(binding: ItemLanguageBinding, item: LanguageViewItem) {

        binding.root.delegate.setBackground(item.background)
    }
}

data class LanguageViewItem(
    val data: Language,

    val name: RichText,
    val image: String,
    val isSelected: Boolean,

    val background: Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        data.id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        name to Payload.NAME,
        background to Payload.THEME,
    )
}