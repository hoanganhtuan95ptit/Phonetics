package com.simple.phonetics.ui.phonetics.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemPhoneticsBinding
import com.simple.phonetics.entities.Phonetics

class PhoneticsAdapter(onItemClick: (View, PhoneticsViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<PhoneticsViewItem, ItemPhoneticsBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
    }

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem) {
        super.bind(binding, viewType, position, item)

        refreshIpa(binding, item)
        refreshText(binding, item)
    }

    private fun refreshIpa(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {

        binding.tvIpa.setText(item.ipa)
    }

    private fun refreshText(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {

        binding.tvText.setText(item.text)
    }
}

data class PhoneticsViewItem(
    val id: String,
    val data: Phonetics,

    var ipa: CharSequence = "",
    var text: CharSequence = ""
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        text to PAYLOAD_TEXT,
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"