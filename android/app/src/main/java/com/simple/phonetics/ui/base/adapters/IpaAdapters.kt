package com.simple.phonetics.ui.base.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setSize
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemIpaBinding
import com.simple.phonetics.entities.Ipa

class IpaAdapters(onItemClick: (View, IpaViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<IpaViewItem, ItemIpaBinding>(onItemClick) {

    override fun bind(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(Payload.SIZE)) refreshSize(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
        if (payloads.contains(Payload.MARGIN)) refreshMargin(binding, item)
        if (payloads.contains(Payload.BACKGROUND)) refreshBackground(binding, item)
    }

    override fun bind(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem) {
        super.bind(binding, viewType, position, item)

        binding.root.transitionName = item.id

        refreshIpa(binding, item)
        refreshSize(binding, item)
        refreshText(binding, item)
        refreshMargin(binding, item)
        refreshBackground(binding, item)
    }

    private fun refreshIpa(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.tvIpa.text = item.ipa
    }

    private fun refreshSize(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setSize(item.size)
    }

    private fun refreshText(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.tvWord.text = item.text
    }

    private fun refreshMargin(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setMargin(item.margin)
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

    val size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val background: Background = DEFAULT_BACKGROUND
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        text to PAYLOAD_TEXT,

        size to Payload.SIZE,
        margin to Payload.MARGIN,
        background to Payload.BACKGROUND
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
