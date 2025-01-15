package com.simple.phonetics.ui.language.adapters

import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemLanguageStateBinding

class LanguageStateAdapter : ViewItemAdapter<LanguageStateViewItem, ItemLanguageStateBinding>() {

    override fun bind(binding: ItemLanguageStateBinding, viewType: Int, position: Int, item: LanguageStateViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.NAME)) {

            bindingName(binding, item)
        }
    }

    override fun bind(binding: ItemLanguageStateBinding, viewType: Int, position: Int, item: LanguageStateViewItem) {
        super.bind(binding, viewType, position, item)

        bindingName(binding, item)
    }

    private fun bindingName(binding: ItemLanguageStateBinding, item: LanguageStateViewItem) {

        binding.tvValue.setText(item.name)
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