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
import com.simple.analytics.logAnalytics
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
import com.simple.phonetics.utils.width
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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
            logAnalytics("campaign_use_click")
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemCampaignBinding, viewType: Int, position: Int, item: CampaignViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.setSize(item.size)

        binding.ivCampaign.setImage(item.image)

        binding.tvTitle.setText(item.text)
        binding.tvMessage.setText(item.message)

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

    override var size: Size = Size()
) : ViewItem, SizeViewItem {

    override fun measureSize(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        val titleHeight = measureTextViewHeight(
            text = text.textChar,
            maxWidth = appSize.width - DP.DP_16 - DP.DP_16 - DP.DP_60 - DP.DP_16 - DP.DP_16 - DP.DP_16,
            metrics = style["TextBody1"]?: return Size()
        )

        val messageHeight = measureTextViewHeight(
            text = message.textChar,
            maxWidth = appSize.width - DP.DP_16 - DP.DP_16 - DP.DP_60 - DP.DP_16 - DP.DP_16 - DP.DP_16,
            metrics = style["TextBody2"] ?: return Size()
        )

        return Size(
            width = size.width,
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

    var size: Size

    fun measure(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>) {

        if (size.height != ViewGroup.LayoutParams.WRAP_CONTENT && size.width != ViewGroup.LayoutParams.WRAP_CONTENT) {

            return
        }

        size = measureSize(appSize, style)
    }

    fun measureSize(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>): Size
}

fun measureTextViewWidth(
    text: CharSequence,
    maxWidth: Int,
    metrics: TextViewMetrics
): Int {

    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = metrics.textSizePx
        typeface = metrics.typeface
        textScaleX = metrics.textScaleX
        letterSpacing = metrics.letterSpacing
    }

    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(metrics.lineSpacingExtra, metrics.lineSpacingMultiplier)
        .setIncludePad(metrics.includeFontPadding)
        .build()

    var maxLineWidth = 0f
    for (i in 0 until layout.lineCount) {
        maxLineWidth = max(maxLineWidth, layout.getLineWidth(i))
    }

    return ceil(maxLineWidth).toInt()
}

fun measureTextViewHeight(
    text: CharSequence,
    maxWidth: Int,
    metrics: TextViewMetrics,
    maxLines: Int? = null
): Int {

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = metrics.textSizePx
        typeface = metrics.typeface
        textScaleX = metrics.textScaleX
        letterSpacing = metrics.letterSpacing
    }

    val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(metrics.lineSpacingExtra, metrics.lineSpacingMultiplier)
        .setIncludePad(metrics.includeFontPadding)
        .build()

    val maxLines = maxLines ?: Int.MAX_VALUE
    val lineCount = min(staticLayout.lineCount, maxLines)

    // Nếu lineCount = 0 → không có gì để đo
    if (lineCount == 0) return 0

    val height = staticLayout.getLineBottom(lineCount - 1)

    return height
}