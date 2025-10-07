package com.simple.phonetics.ui.base.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.phonetics.campaign.ui.adapters.SizeViewItem
import com.phonetics.campaign.ui.adapters.measureTextViewHeight
import com.phonetics.size.TextViewMetrics
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.dao.entities.Ipa
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemIpaBinding
import com.unknown.size.uitls.exts.width

@ItemAdapter
class IpaAdapters(private val onItemClickV2: ((View, IpaViewItem) -> Unit)? = null) : ViewItemAdapter<IpaViewItem, ItemIpaBinding>() {

    override val viewItemClass: Class<IpaViewItem> by lazy {
        IpaViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemIpaBinding {
        return ItemIpaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemIpaBinding> {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setDebouncedClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            onItemClickV2?.invoke(view, viewItem)
            sendEvent(EventName.IPA_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        binding.root.transitionName = item.id

        if (payloads.contains(Payload.SIZE)) refreshSize(binding, item)
        if (payloads.contains(Payload.MARGIN)) refreshMargin(binding, item)
        if (payloads.contains(Payload.BACKGROUND)) refreshBackground(binding, item)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)
    }

    override fun onBindViewHolder(binding: ItemIpaBinding, viewType: Int, position: Int, item: IpaViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.transitionName = item.id

        refreshSize(binding, item)
        refreshMargin(binding, item)
        refreshPadding(binding, item)
        refreshBackground(binding, item)

        refreshIpa(binding, item)

        refreshText(binding, item)
    }

    private fun refreshSize(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setSize(item.size)
    }

    private fun refreshIpa(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.tvIpa.setText(item.ipa)
    }

    private fun refreshText(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.tvWord.setText(item.text)
    }

    private fun refreshMargin(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setMargin(item.margin)
    }

    private fun refreshPadding(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setPadding(item.padding)
    }

    private fun refreshBackground(binding: ItemIpaBinding, item: IpaViewItem) {
        binding.root.setBackground(item.background)
    }
}

data class IpaViewItem(
    val id: String,
    val data: Ipa,

    val ipa: RichText = emptyText(),
    val text: RichText = emptyText(),

    override var size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val padding: Padding = Padding(paddingVertical = DP.DP_16),
    val background: Background = DEFAULT_BACKGROUND
) : ViewItem, SizeViewItem {


    override fun measureSize(size: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {


        val ipaMetrics = style["TextAppearance_MaterialComponents_Headline6"] ?: return this.size
        val textMetrics = style["TextAppearance_MaterialComponents_Body1"] ?: return this.size


        val maxWidth = if (this.size.width < 10) {
            size.width - padding.left - padding.right - margin.left - margin.right
        } else {
            this.size.width - padding.left - padding.right
        }

        val ipaHeight = measureTextViewHeight(
            text = ipa.textChar,
            maxWidth = maxWidth,
            metrics = ipaMetrics
        )

        val textHeight = measureTextViewHeight(
            text = text.textChar,
            maxWidth = maxWidth,
            metrics = textMetrics
        )

        return Size(
            width = maxWidth,
            height = ipaHeight + DP.DP_4 + textHeight + padding.top + padding.bottom
        )
    }

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
