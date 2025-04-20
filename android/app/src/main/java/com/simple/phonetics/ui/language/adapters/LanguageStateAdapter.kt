package com.simple.phonetics.ui.language.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemLanguageStateBinding

@ItemAdapter
class LanguageStateAdapter : ViewItemAdapter<LanguageStateViewItem, ItemLanguageStateBinding>() {

    override val viewItemClass: Class<LanguageStateViewItem> by lazy {
        LanguageStateViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemLanguageStateBinding {
        return ItemLanguageStateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemLanguageStateBinding, viewType: Int, position: Int, item: LanguageStateViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.NAME)) bindingName(binding, item)
    }

    override fun onBindViewHolder(binding: ItemLanguageStateBinding, viewType: Int, position: Int, item: LanguageStateViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        bindingName(binding, item)
    }

    private fun bindingName(binding: ItemLanguageStateBinding, item: LanguageStateViewItem) {
        binding.tvValue.text = item.name
    }
}

data class LanguageStateViewItem(
    val data: String,

    val name: CharSequence,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        data
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        name to Payload.NAME
    )
}

private object Payload {

    const val NAME = "NAME"
}