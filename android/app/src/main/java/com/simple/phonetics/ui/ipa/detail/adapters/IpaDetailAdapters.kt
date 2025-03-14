package com.simple.phonetics.ui.ipa.detail.adapters

import android.view.View
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Payload
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemIpaDetailBinding
import com.simple.phonetics.entities.Ipa

class IpaDetailAdapters(onItemClick: (View, IpaDetailViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<IpaDetailViewItem, ItemIpaDetailBinding>(onItemClick) {

    override fun bind(binding: ItemIpaDetailBinding, viewType: Int, position: Int, item: IpaDetailViewItem, payloads: MutableList<Any>) {
        super.bind(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_IPA)) refreshIpa(binding, item)
        if (payloads.contains(PAYLOAD_BACKGROUND)) refreshBackground(binding, item)

        if (payloads.contains(Payload.IMAGE)) refreshImage(binding, item)
        if (payloads.contains(Payload.LOADING_STATUS)) refreshLoadingStatus(binding, item)
    }

    override fun bind(binding: ItemIpaDetailBinding, viewType: Int, position: Int, item: IpaDetailViewItem) {
        super.bind(binding, viewType, position, item)

        refreshIpa(binding, item)
        refreshImage(binding, item)
        refreshBackground(binding, item)
        refreshLoadingStatus(binding, item)
    }

    private fun refreshIpa(binding: ItemIpaDetailBinding, item: IpaDetailViewItem) {
        binding.tvIpa.text = item.ipa
    }

    private fun refreshImage(binding: ItemIpaDetailBinding, item: IpaDetailViewItem) {
        binding.ivVolume.setImageResource(item.image)
    }

    private fun refreshBackground(binding: ItemIpaDetailBinding, item: IpaDetailViewItem) {
        binding.frameContent.delegate.setBackground(item.background)
    }

    private fun refreshLoadingStatus(binding: ItemIpaDetailBinding, item: IpaDetailViewItem) {
        binding.progress.setVisible(item.isShowLoading)
    }
}

data class IpaDetailViewItem(
    val id: String,

    val data: Ipa,

    val ipa: CharSequence = "",

    val image: Int = R.drawable.img_volume,
    val isShowLoading: Boolean = false,

    val background: Background
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to PAYLOAD_IPA,
        background to PAYLOAD_BACKGROUND,

        image to Payload.IMAGE,
        isShowLoading to Payload.LOADING_STATUS
    )
}

private const val PAYLOAD_IPA = "PAYLOAD_IPA"
private const val PAYLOAD_BACKGROUND = "PAYLOAD_BACKGROUND"