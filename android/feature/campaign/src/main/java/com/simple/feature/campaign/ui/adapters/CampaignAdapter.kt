package com.simple.feature.campaign.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
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
import com.simple.feature.campaign.entities.Campaign
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemCampaignBinding
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.adapters.measureTextViewHeight
import com.simple.phonetics.utils.TextViewMetrics
import com.unknown.size.uitls.exts.width
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