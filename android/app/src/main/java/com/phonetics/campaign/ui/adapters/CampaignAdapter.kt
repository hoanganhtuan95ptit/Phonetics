package com.phonetics.campaign.ui.adapters

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.ViewGroup
import com.phonetics.campaign.entities.Campaign
import com.phonetics.size.TextViewMetrics
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.deeplink.sendDeeplink
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemCampaignBinding
import kotlin.math.max

@ItemAdapter
class CampaignAdapter : ViewItemAdapter<CampaignViewItem, ItemCampaignBinding>() {

    override val viewItemClass: Class<CampaignViewItem> by lazy {
        CampaignViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemCampaignBinding {
        return ItemCampaignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemCampaignBinding> {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setOnClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setOnClickListener

            sendDeeplink(viewItem.data.deeplink.orEmpty())
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemCampaignBinding, viewType: Int, position: Int, item: CampaignViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.ivCampaign.setImage(item.image)

        binding.tvTitle.setText(item.text)
        binding.tvMessage.setText(item.message)

        binding.root.setSize(item.size)
        binding.root.setBackground(item.background)
    }
}

data class CampaignViewItem(
    val id: String,
    val data: Campaign,

    val image: String,

    val text: RichText = emptyText(),
    val message: RichText = emptyText(),

    val background: Background,

    override val size: Size = Size()
) : ViewItem, SizeViewItem {

    fun measure(size: Map<String, Int>, style: Map<String, TextViewMetrics>):CampaignViewItem = copy(
        size = measureSize(size, style)
    )

    override fun measureSize(size: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        val titleHeight = measureTextViewHeight(
            text = text.textChar,
            availableWidth =  size["width"].orZero() - DP.DP_16 - DP.DP_16 - DP.DP_60 - DP.DP_16 - DP.DP_16 - DP.DP_16,
            metrics = style["TextBody1"]?: return Size()
        )

        val messageHeight = measureTextViewHeight(
            text = message.textChar,
            availableWidth = size["width"].orZero() - DP.DP_16 - DP.DP_16 - DP.DP_60 - DP.DP_16 - DP.DP_16 - DP.DP_16,
            metrics = style["TextBody2"] ?: return Size()
        )

        return Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = max(DP.DP_60, titleHeight + DP.DP_4 + messageHeight) + DP.DP_16 + DP.DP_16
        )
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
    )
}

interface SizeViewItem {

    val size: Size

    fun measureSize(size: Map<String, Int>, style: Map<String, TextViewMetrics>): Size

    fun measureTextViewHeight(
        text: CharSequence,
        availableWidth: Int,
        metrics: TextViewMetrics
    ): Int {

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = metrics.textSizePx
            typeface = metrics.typeface
        }

        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, availableWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(metrics.lineSpacingExtra, metrics.lineSpacingMultiplier)
            .setIncludePad(metrics.includeFontPadding)
            .build()

        return staticLayout.height + metrics.lineSpacingExtra.toInt()
    }
}