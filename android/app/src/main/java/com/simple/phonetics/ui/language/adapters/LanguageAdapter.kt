package com.simple.phonetics.ui.language.adapters

import android.view.View
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.round.setBackground
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemLanguageBinding
import com.simple.phonetics.entities.Language

class LanguageAdapter(onItemClick: (View, LanguageViewItem) -> Unit) : ViewItemAdapter<LanguageViewItem, ItemLanguageBinding>(onItemClick) {

    override fun bind(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.NAME)) {

            refreshName(binding, item)
        }

        if (payloads.contains(Payload.THEME)) {

            refreshTheme(binding, item)
        }
    }

    override fun bind(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem) {
        super.bind(binding, viewType, position, item)

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

    val name: CharSequence,
    val image: String,
    val isSelected: Boolean,

    val background: com.simple.coreapp.ui.view.round.Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        data.id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        name to Payload.NAME,
        background to Payload.THEME,
    )
}