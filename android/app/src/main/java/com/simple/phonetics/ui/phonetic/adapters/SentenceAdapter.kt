package com.simple.phonetics.ui.phonetic.adapters

import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemSentenceBinding
import com.simple.phonetics.entities.Sentence
import com.tuanha.adapter.annotation.AdapterPreview

@AdapterPreview
class SentenceAdapter : ViewItemAdapter<SentenceViewItem, ItemSentenceBinding>() {

    override fun bind(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.TEXT)) refreshText(binding, item)
        if (payloads.contains(PAYLOAD_IS_LAST)) refreshIsLast(binding, item)
    }

    override fun bind(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
        refreshIsLast(binding, item)
    }

    private fun refreshText(binding: ItemSentenceBinding, item: SentenceViewItem) {
        binding.tvText.text = item.text
    }

    private fun refreshIsLast(binding: ItemSentenceBinding, item: SentenceViewItem) {
        binding.vDivider1.setVisible(!item.isLast)
    }
}

data class SentenceViewItem(
    val id: String,
    val data: Sentence,

    var text: CharSequence = "",

    var isLast: Boolean = false
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
        isLast to PAYLOAD_IS_LAST
    )
}

private const val PAYLOAD_IS_LAST = "PAYLOAD_IS_LAST"