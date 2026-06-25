package com.simple.phonetics.ui.common.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemTextViewBinding

@ItemAdapter
class TextViewAdapter(onItemClick: (View, TextViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<TextViewItem, ItemTextViewBinding>(onItemClick) {

    override val viewItemClass: Class<TextViewItem> by lazy {
        TextViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemTextViewBinding {
        return ItemTextViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

//    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemTextViewBinding> {
//        val viewHolder = super.createViewHolder(parent, viewType)
//        viewHolder.binding.root.transitionName
//        return viewHolder
//    }

    override fun onBindViewHolder(binding: ItemTextViewBinding, viewType: Int, position: Int, item: TextViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_TEXT)) refreshText(binding, item)

        if (payloads.contains(PAYLOAD_TEXT_SIZE)) refreshTextSize(binding, item)
        if (payloads.contains(PAYLOAD_TEXT_MARGIN)) refreshTextMargin(binding, item)
        if (payloads.contains(PAYLOAD_TEXT_PADDING)) refreshTextPadding(binding, item)
        if (payloads.contains(PAYLOAD_TEXT_BACKGROUND)) refreshTextBackground(binding, item)

        if (payloads.contains(PAYLOAD_SIZE)) refreshSize(binding, item)
        if (payloads.contains(PAYLOAD_MARGIN)) refreshMargin(binding, item)
        if (payloads.contains(PAYLOAD_PADDING)) refreshPadding(binding, item)
        if (payloads.contains(PAYLOAD_BACKGROUND)) refreshBackground(binding, item)
    }

    override fun onBindViewHolder(binding: ItemTextViewBinding, viewType: Int, position: Int, item: TextViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.transitionName = item.id

        refreshText(binding, item)

        refreshTextSize(binding, item)
        refreshTextMargin(binding, item)
        refreshTextPadding(binding, item)
        refreshTextBackground(binding, item)

        refreshSize(binding, item)
        refreshMargin(binding, item)
        refreshPadding(binding, item)
        refreshBackground(binding, item)
    }

    private fun refreshText(binding: ItemTextViewBinding, item: TextViewItem) {
        binding.tvText.setText(item.text)
    }

    private fun refreshTextSize(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.tvText.setSize(item.textSize)
    }

    private fun refreshTextMargin(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.tvText.setMargin(item.textMargin)
    }

    private fun refreshTextPadding(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.tvText.setPadding(item.textPadding)
    }

    private fun refreshTextBackground(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.tvText.setBackground(item.textBackground)
    }

    private fun refreshSize(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.root.setSize(item.size)
    }

    private fun refreshMargin(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.root.setMargin(item.margin)
    }

    private fun refreshPadding(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.root.setPadding(item.padding)
    }

    private fun refreshBackground(binding: ItemTextViewBinding, item: TextViewItem) {
//        binding.root.setBackground(item.background)
    }
}

data class TextViewItem(
    val id: String,
    val data: Any? = null,

    var text: RichText = emptyText(),

    val textSize: Size = DEFAULT_SIZE,
    val textMargin: Margin = DEFAULT_MARGIN,
    val textPadding: Padding = DEFAULT_PADDING,
    val textBackground: Background = DEFAULT_BACKGROUND,

    val size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val padding: Padding = DEFAULT_PADDING,
    val background: Background = DEFAULT_BACKGROUND,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to PAYLOAD_TEXT,

        textSize to PAYLOAD_TEXT_SIZE,
        textMargin to PAYLOAD_TEXT_MARGIN,
        textPadding to PAYLOAD_TEXT_PADDING,
        textBackground to PAYLOAD_TEXT_BACKGROUND,

        size to PAYLOAD_SIZE,
        margin to PAYLOAD_MARGIN,
        padding to PAYLOAD_PADDING,
        background to PAYLOAD_BACKGROUND,
    )
}

private const val PAYLOAD_TEXT = "PAYLOAD_TEXT"
private const val PAYLOAD_TEXT_STYLE = "PAYLOAD_TEXT_STYLE"

private const val PAYLOAD_TEXT_SIZE = "PAYLOAD_TEXT_SIZE"
private const val PAYLOAD_TEXT_MARGIN = "PAYLOAD_TEXT_MARGIN"
private const val PAYLOAD_TEXT_PADDING = "PAYLOAD_TEXT_PADDING"
private const val PAYLOAD_TEXT_BACKGROUND = "PAYLOAD_TEXT_BACKGROUND"

private const val PAYLOAD_SIZE = "PAYLOAD_SIZE"
private const val PAYLOAD_MARGIN = "PAYLOAD_MARGIN"
private const val PAYLOAD_PADDING = "PAYLOAD_PADDING"
private const val PAYLOAD_BACKGROUND = "PAYLOAD_BACKGROUND"
