package com.simple.phonetics.ui.phonetics.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemPhoneticsBinding
import com.simple.phonetics.entities.Phonetics

class PhoneticsAdapter(onItemClick: (View, PhoneticsViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<PhoneticsViewItem, ItemPhoneticsBinding>(onItemClick) {

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) {

            refreshIpa(binding, item)
        }

        if (payloads.contains(PAYLOAD_TEXT)) {

            refreshText(binding, item)
        }

        if (payloads.contains(PAYLOAD_IS_MULTI_IPA)) {

            refreshIsMultiIpa(binding, item)
        }
    }

    override fun bind(binding: ItemPhoneticsBinding, viewType: Int, position: Int, item: PhoneticsViewItem) {
        super.bind(binding, viewType, position, item)

        refreshIpa(binding, item)
        refreshText(binding, item)
        refreshIsMultiIpa(binding, item)
    }

    private fun refreshIpa(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {

        binding.tvIpa.setText(item.ipa)
    }

    private fun refreshText(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {

        binding.tvText.setText(item.text)
    }

    private fun refreshIsMultiIpa(binding: ItemPhoneticsBinding, item: PhoneticsViewItem) {

        binding.tvIpa.isSelected = item.isMultiIpa
    }
}

data class PhoneticsViewItem(
    val id: String,
    val data: Phonetics,

    var ipa: String = "",
    var text: String = "",

    var isMultiIpa: Boolean = false
) : ViewItem {

//    fun refresh(ipaCode: String) = apply {
//
//        val codeAndIpa = data.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }
//
//        ipa = (codeAndIpa?.get(ipaCode) ?: codeAndIpa?.toList()?.first()?.second)?.firstOrNull()?.toText() ?: emptyText()
//
//        text = data.text.toText()
//
//        isMultiIpa = data.ipa.size > 1
//    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        text to PAYLOAD_TEXT,
        isMultiIpa to PAYLOAD_IS_MULTI_IPA
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_MULTI_IPA = "PAYLOAD_IS_MULTI_IPA"