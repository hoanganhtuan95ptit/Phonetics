package com.simple.phonetics.ui.base.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.ui.view.round.setBackground
import com.simple.phonetics.databinding.ItemIpaBinding
import com.simple.phonetics.entities.Ipa

class IpaAdapters(onItemClick: (View, IpaViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<IpaViewItem, ItemIpaBinding>(onItemClick) {

    override fun bind(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
        if (payloads.contains(PAYLOAD_BACKGROUND)) refreshBackground(binding, item)
    }

    override fun bind(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem) {
        super.bind(binding, viewType, position, item)

        binding.root.transitionName = item.id

        refreshIpa(binding, item)
        refreshText(binding, item)
        refreshBackground(binding, item)
    }

    private fun refreshIpa(binding: ItemIpaBinding, item: IpaViewItem) {

        binding.tvIpa.text = item.ipa
    }

    private fun refreshText(binding: ItemIpaBinding, item: IpaViewItem) {

        binding.tvWord.text = item.text
    }

    private fun refreshBackground(binding: ItemIpaBinding, item: IpaViewItem) {

        binding.root.delegate.setBackground(item.background)
    }
}

data class IpaViewItem(
    val id: String,

    val data: Ipa,

    val ipa: CharSequence = "",
    val text: CharSequence = "",

    val background: Background
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        text to PAYLOAD_TEXT,
        background to PAYLOAD_BACKGROUND
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_BACKGROUND = "PAYLOAD_BACKGROUND"