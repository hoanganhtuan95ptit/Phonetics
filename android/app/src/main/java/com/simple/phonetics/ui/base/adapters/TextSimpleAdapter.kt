package com.simple.phonetics.ui.base.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.phonetics.campaign.ui.adapters.SizeViewItem
import com.phonetics.campaign.ui.adapters.measureTextViewHeight
import com.phonetics.campaign.ui.adapters.measureTextViewWidth
import com.phonetics.size.TextViewMetrics
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemTextSimpleBinding
import com.unknown.size.uitls.exts.width

@ItemAdapter
class TextSimpleAdapter() : ViewItemAdapter<TextSimpleViewItem, ItemTextSimpleBinding>() {

    override val viewItemClass: Class<TextSimpleViewItem> by lazy {
        TextSimpleViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemTextSimpleBinding {
        return ItemTextSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemTextSimpleBinding> {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setDebouncedClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            sendEvent(EventName.TEXT_SIMPLE_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemTextSimpleBinding, viewType: Int, position: Int, item: TextSimpleViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        binding.root.transitionName = item.id

        if (payloads.contains("size")) refreshSize(binding, item)
        if (payloads.contains("margin")) refreshMargin(binding, item)
        if (payloads.contains("padding")) refreshPadding(binding, item)
        if (payloads.contains("background")) refreshBackground(binding, item)

        if (payloads.contains("text")) refreshText(binding, item)
        if (payloads.contains("textStyle")) refreshTextStyle(binding, item)

        if (payloads.contains("textSize")) refreshTextSize(binding, item)
        if (payloads.contains("textMargin")) refreshTextMargin(binding, item)
        if (payloads.contains("textPadding")) refreshTextPadding(binding, item)
        if (payloads.contains("textBackground")) refreshTextBackground(binding, item)
    }

    override fun onBindViewHolder(binding: ItemTextSimpleBinding, viewType: Int, position: Int, item: TextSimpleViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.transitionName = item.id

        refreshSize(binding, item)
        refreshMargin(binding, item)
        refreshPadding(binding, item)
        refreshBackground(binding, item)

        refreshText(binding, item)
        refreshTextStyle(binding, item)

        refreshTextSize(binding, item)
        refreshTextMargin(binding, item)
        refreshTextPadding(binding, item)
        refreshTextBackground(binding, item)
    }

    private fun refreshSize(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.root.setSize(item.size)
    }

    private fun refreshMargin(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.root.setMargin(item.margin)
    }

    private fun refreshPadding(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.root.setPadding(item.padding)
    }

    private fun refreshBackground(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.root.setBackground(item.background)
    }


    private fun refreshText(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setText(item.text)
    }

    private fun refreshTextStyle(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setTextAppearance(item.textStyle)
    }


    private fun refreshTextSize(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setSize(item.textSize)
    }

    private fun refreshTextMargin(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setMargin(item.textMargin)
    }

    private fun refreshTextPadding(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setPadding(item.textPadding)
    }

    private fun refreshTextBackground(binding: ItemTextSimpleBinding, item: TextSimpleViewItem) {
        binding.tvText.setBackground(item.textBackground)
    }
}

data class TextSimpleViewItem(
    val id: String,
    val data: Any? = null,

    var text: RichText = emptyText(),
    var textStyle: Int = R.style.TextBody1,

    val textSize: Size = DEFAULT_SIZE,
    val textMargin: Margin = DEFAULT_MARGIN,
    val textPadding: Padding = DEFAULT_PADDING,
    val textBackground: Background = DEFAULT_BACKGROUND,

    override var size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val padding: Padding = DEFAULT_PADDING,
    val background: Background = DEFAULT_BACKGROUND
) : ViewItem, SizeViewItem {

    override fun measureSize(size: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        val styleMap = R.style::class.java.fields.toList().associateBy({ it.getInt(null) }, { it.name })

        val styleName = styleMap[textStyle] ?: return this.size

        val textMetrics = style[styleName] ?: return this.size


        val maxWidth = if (this.size.width < 10) {
            size.width - textPadding.left - textPadding.right - textMargin.left - textMargin.right - padding.left - padding.right - margin.left - margin.right
        } else {
            this.size.width - textPadding.left - textPadding.right - textMargin.left - textMargin.right - padding.left - padding.right
        }

        val textWidth = measureTextViewWidth(
            text = text.textChar,
            maxWidth = maxWidth,
            metrics = textMetrics
        )

        val textHeight = measureTextViewHeight(
            text = text.textChar,
            maxWidth = maxWidth,
            metrics = textMetrics
        )

        return Size(
            width = textWidth + textPadding.left + textPadding.right + textMargin.left + textMargin.right + padding.left + padding.right,
            height = textHeight + textPadding.top + textPadding.bottom + textMargin.top + textMargin.bottom + padding.top + padding.bottom
        )
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to "text",
        textStyle to "textStyle",

        textSize to "textSize",
        textMargin to "textMargin",
        textPadding to "textPadding",
        textBackground to "textBackground",

        size to "size",
        margin to "margin",
        padding to "padding",
        background to "background"
    )
}