package com.simple.phonetics.ui.common.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.image.setImage
import com.simple.phonetic.entities.Phonetic
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemPhoneticsBinding

@ItemAdapter
class PhoneticsAdapter(onItemClick: ((View, PhoneticsViewItem) -> Unit)? = null) : ViewItemAdapter<PhoneticsViewItem, ItemPhoneticsBinding>(onItemClick) {

    override val viewItemClass: Class<PhoneticsViewItem> by lazy {
        PhoneticsViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPhoneticsBinding {
        return ItemPhoneticsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
        if (payloads.contains(PAYLOAD_IMAGE)) refreshImage(binding, item)
        if (payloads.contains(Payload.PADDING)) refreshPadding(binding, item)
    }

    override fun onBindViewHolder(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshIpa(binding, item)
        refreshText(binding, item)
        refreshImage(binding, item)
        refreshPadding(binding, item)
    }

    private fun refreshIpa(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.tvIpa.setText(item.ipa)
    }

    private fun refreshText(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.tvText.setText(item.text)
    }

    private fun refreshImage(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.ivDown.setImage(item.image)
        binding.ivDown.setVisible(item.image != 0)
    }

    private fun refreshPadding(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.root.setPadding(item.padding)
    }
}

data class PhoneticsViewItem(
    val id: String,
    val data: Phonetic,

    var ipa: RichText = emptyText(),
    var text: RichText = emptyText(),

    var image: Int = 0,

    val padding: Padding = DEFAULT_PADDING
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        text to PAYLOAD_TEXT,
        image to PAYLOAD_IMAGE,

        padding to Payload.PADDING
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IMAGE = "PAYLOAD_DOWN"