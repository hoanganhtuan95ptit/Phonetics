package com.simple.phonetics.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.phonetics.utils.TextViewMetrics
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemHistoryBinding
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.adapters.measureTextViewHeight
import com.unknown.size.uitls.exts.width

@ItemAdapter
class HistoryAdapter(private val onItemClickV2: ((View, HistoryViewItem) -> Unit)? = null) : ViewItemAdapter<HistoryViewItem, ItemHistoryBinding>() {

    override val viewItemClass: Class<HistoryViewItem> by lazy {
        HistoryViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemHistoryBinding {
        return ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemHistoryBinding> {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setOnClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setOnClickListener

            onItemClickV2?.invoke(view, viewItem)
            sendEvent(EventName.HISTORY_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.SIZE)) refreshSize(binding, item)
        if (payloads.contains(Payload.TEXT)) refreshText(binding, item)
    }

    override fun onBindViewHolder(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshSize(binding, item)
        refreshText(binding, item)
    }

    private fun refreshSize(binding: ItemHistoryBinding, item: HistoryViewItem) {
        binding.root.setSize(item.size)
    }

    private fun refreshText(binding: ItemHistoryBinding, item: HistoryViewItem) {
        binding.tvText.setText(item.text)
    }
}

data class HistoryViewItem(
    val id: String,

    val text: RichText = emptyText(),

    override var size: Size = DEFAULT_SIZE,
) : ViewItem, SizeViewItem {

    override fun measureSize(size: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        val textMetrics = style["TextBody1"] ?: return this.size

        val maxWidth = size.width - DP.DP_16 - DP.DP_20 - DP.DP_16 - DP.DP_16

        val textHeight = measureTextViewHeight(
            text = text.textChar,
            maxWidth = maxWidth,
            metrics = textMetrics,
            maxLines = 3
        )

        return Size(
            width = size.width,
            height = DP.DP_8 + textHeight + DP.DP_8
        )
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
        size to Payload.SIZE,
    )
}