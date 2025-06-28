package com.simple.phonetics.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemSentenceBinding
import com.simple.phonetics.entities.Sentence

@ItemAdapter
class SentenceAdapter : ViewItemAdapter<SentenceViewItem, ItemSentenceBinding>() {

    override val viewItemClass: Class<SentenceViewItem> by lazy {
        SentenceViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemSentenceBinding {
        return ItemSentenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemSentenceBinding>? {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setOnClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setOnClickListener

            sendEvent(EventName.SENTENCE_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.TEXT)) refreshText(binding, item)
        if (payloads.contains(PAYLOAD_IS_LAST)) refreshIsLast(binding, item)
    }

    override fun onBindViewHolder(binding: ItemSentenceBinding, viewType: Int, position: Int, item: SentenceViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

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

    var text: RichText = emptyText(),

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