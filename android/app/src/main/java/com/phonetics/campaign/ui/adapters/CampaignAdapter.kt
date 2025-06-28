package com.phonetics.campaign.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.phonetics.campaign.entities.Campaign
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.deeplink.sendDeeplink
import com.simple.image.setImage
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemCampaignBinding

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

        binding.root.delegate.setBackground(item.background)
    }
}

data class CampaignViewItem(
    val id: String,
    val data: Campaign,

    val image: String,

    val text: RichText = emptyText(),
    val message: RichText = emptyText(),

    val background: Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
    )
}