package com.simple.phonetics.ui.language.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.image.setImage
import com.simple.phonetics.Param
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemLanguageBinding
import com.simple.phonetics.entities.Language

class LanguageAdapter(onItemClick: (View, LanguageViewItem) -> Unit) : ViewItemAdapter<LanguageViewItem, ItemLanguageBinding>(onItemClick) {

    override fun bind(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.SELECTED)) {

            bindingSelected(binding, item)
        }
    }

    override fun bind(binding: ItemLanguageBinding, viewType: Int, position: Int, item: LanguageViewItem) {
        super.bind(binding, viewType, position, item)

        binding.tvName.setText(item.name)
        binding.ivFlag.setImage(item.image)

        bindingSelected(binding, item)
    }

    private fun bindingSelected(binding: ItemLanguageBinding, item: LanguageViewItem) {

        binding.root.isSelected = item.isSelected
        binding.tvName.isSelected = item.isSelected
    }
}

data class LanguageViewItem(
    val data: Language,

    val name: String,
    val image: String,

    val isSelected: Boolean
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        data.id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        isSelected to Payload.SELECTED
    )
}