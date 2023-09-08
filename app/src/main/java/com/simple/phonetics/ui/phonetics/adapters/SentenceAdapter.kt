package com.simple.phonetics.ui.phonetics.adapters

import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.ViewItemCloneable
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.setText
import com.simple.coreapp.utils.extentions.setVisible
import com.simple.coreapp.utils.extentions.toText
import com.simple.state.toSuccess
import com.simple.phonetics.databinding.ItemSentenceBinding
import com.simple.phonetics.domain.entities.Sentence

class SentenceAdapter : ViewItemAdapter<SentenceViewItem, ItemSentenceBinding>() {

    override fun bind(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) {

            refreshText(binding, item)
        }

        if (payloads.contains(PAYLOAD_IS_LAST)) {

            refreshIsLast(binding, item)
        }
    }

    override fun bind(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem) {
        super.bind(binding, viewType, position, item)

        refreshText(binding, item)
        refreshIsLast(binding, item)
    }

    private fun refreshText(binding: ItemSentenceBinding, item: SentenceViewItem) {

        binding.tvText.setText(item.text)
    }

    private fun refreshIsLast(binding: ItemSentenceBinding, item: SentenceViewItem) {

        binding.vDivider1.setVisible(!item.isLast)
    }
}

data class SentenceViewItem(
    val id: String,
    val data: Sentence,

    var text: Text<*> = emptyText(),
    var isLast: Boolean = false
) : ViewItemCloneable {

    fun refresh(isLast: Boolean) = apply {

        text = data.translateState.toSuccess()?.data?.toText() ?: "failed".toText()

        this.isLast = isLast
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,
        isLast to PAYLOAD_IS_LAST
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_IS_LAST = "PAYLOAD_IS_LAST"