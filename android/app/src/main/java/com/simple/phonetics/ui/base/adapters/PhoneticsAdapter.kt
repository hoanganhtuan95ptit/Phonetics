package com.simple.phonetics.ui.base.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.utils.ext.setVisible
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemPhoneticsBinding
import com.simple.phonetics.entities.Phonetics

class PhoneticsAdapter(onItemClick: ((View, PhoneticsViewItem) -> Unit)? = null) : ViewItemAdapter<PhoneticsViewItem, ItemPhoneticsBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
        if (payloads.contains(PAYLOAD_IMAGE)) refreshImage(binding, item)
        if (payloads.contains(Payload.PADDING)) refreshPadding(binding, item)
    }

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem) {
        super.bind(binding, viewType, position, item)

        refreshIpa(binding, item)
        refreshText(binding, item)
        refreshImage(binding, item)
        refreshPadding(binding, item)
    }

    private fun refreshIpa(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.tvIpa.text = item.ipa
    }

    private fun refreshText(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {
        binding.tvText.text = item.text
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
    val data: Phonetics,

    var ipa: CharSequence = "",
    var text: CharSequence = "",

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