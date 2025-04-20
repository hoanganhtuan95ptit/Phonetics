package com.simple.phonetics.ui.base.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tuanha.adapter.ViewItemAdapter
import com.tuanha.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.setVisible
import com.simple.image.setImage
import com.simple.phonetics.databinding.ItemImageStateBinding
import com.simple.phonetics.databinding.ItemSentenceBinding
import com.simple.phonetics.ui.home.adapters.SentenceViewItem

class ImageStateAdapter(onItemClick: (View, ImageStateViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<ImageStateViewItem, ItemImageStateBinding>(onItemClick) {

    override val viewItemClass: Class<ImageStateViewItem> by lazy {
        ImageStateViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemImageStateBinding {
        return ItemImageStateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemImageStateBinding, viewType: Int, position: Int, item: ImageStateViewItem, payloads: MutableList<Any>) {

        if (payloads.contains(PAYLOAD_IMAGE)) refreshImage(binding, item)
        if (payloads.contains(PAYLOAD_LOADING)) refreshLoading(binding, item)

        if (payloads.contains(PAYLOAD_SIZE)) refreshSize(binding, item)
        if (payloads.contains(PAYLOAD_MARGIN)) refreshMargin(binding, item)
        if (payloads.contains(PAYLOAD_PADDING)) refreshPadding(binding, item)
        if (payloads.contains(PAYLOAD_BACKGROUND)) refreshBackground(binding, item)

        if (payloads.contains(PAYLOAD_IMAGE_SIZE)) refreshImageSize(binding, item)
        if (payloads.contains(PAYLOAD_IMAGE_MARGIN)) refreshImageMargin(binding, item)
        if (payloads.contains(PAYLOAD_IMAGE_PADDING)) refreshImagePadding(binding, item)
        if (payloads.contains(PAYLOAD_IMAGE_BACKGROUND)) refreshImageBackground(binding, item)
    }

    override fun onBindViewHolder(binding: ItemImageStateBinding, viewType: Int, position: Int, item: ImageStateViewItem) {

        binding.root.transitionName = item.id

        refreshImage(binding, item)
        refreshLoading(binding, item)

        refreshSize(binding, item)
        refreshMargin(binding, item)
        refreshPadding(binding, item)
        refreshBackground(binding, item)

        refreshImageSize(binding, item)
        refreshImageMargin(binding, item)
        refreshImagePadding(binding, item)
        refreshImageBackground(binding, item)
    }

    private fun refreshImage(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        if (item.anim != null) {
            binding.ivImage.setAnimation(item.anim)
            binding.ivImage.playAnimation()
        }
        if (item.image != null) {
            binding.ivImage.setImage(item.image)
        }
    }

    private fun refreshLoading(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.progressBar.setVisible(item.isLoading)
    }

    private fun refreshSize(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.root.setSize(item.size)
    }

    private fun refreshMargin(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.root.setMargin(item.margin)
    }

    private fun refreshPadding(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.root.setPadding(item.padding)
    }

    private fun refreshBackground(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.root.delegate.setBackground(item.background)
    }

    private fun refreshImageSize(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.ivImage.setSize(item.imageSize)
    }

    private fun refreshImageMargin(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.ivImage.setMargin(item.imageMargin)
    }

    private fun refreshImagePadding(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.ivImage.setPadding(item.imagePadding)
    }

    private fun refreshImageBackground(binding: ItemImageStateBinding, item: ImageStateViewItem) {

        binding.ivImage.delegate.setBackground(item.imageBackground)
    }
}

data class ImageStateViewItem(
    val id: String,

    val data: Any? = null,

    val anim: Int? = null,
    val image: Int? = null,

    val isLoading: Boolean = false,

    val size: Size? = null,
    val margin: Margin? = null,
    val padding: Padding? = null,
    val background: Background? = null,

    val imageSize: Size? = null,
    val imageMargin: Margin? = null,
    val imagePadding: Padding? = null,
    val imageBackground: Background? = null,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(

        (anim ?: image ?: PAYLOAD_IMAGE) to PAYLOAD_IMAGE,

        isLoading to PAYLOAD_LOADING,

        (size ?: PAYLOAD_SIZE) to PAYLOAD_SIZE,
        (margin ?: PAYLOAD_MARGIN) to PAYLOAD_MARGIN,
        (padding ?: PAYLOAD_PADDING) to PAYLOAD_PADDING,
        (background ?: PAYLOAD_BACKGROUND) to PAYLOAD_BACKGROUND,

        (imageSize ?: PAYLOAD_IMAGE_SIZE) to PAYLOAD_IMAGE_SIZE,
        (imageMargin ?: PAYLOAD_IMAGE_MARGIN) to PAYLOAD_IMAGE_MARGIN,
        (imagePadding ?: PAYLOAD_IMAGE_PADDING) to PAYLOAD_IMAGE_PADDING,
        (imageBackground ?: PAYLOAD_IMAGE_BACKGROUND) to PAYLOAD_IMAGE_BACKGROUND
    )
}

private const val PAYLOAD_IMAGE = "PAYLOAD_IMAGE"
private const val PAYLOAD_LOADING = "PAYLOAD_LOADING"

private const val PAYLOAD_SIZE = "PAYLOAD_SIZE"
private const val PAYLOAD_MARGIN = "PAYLOAD_MARGIN"
private const val PAYLOAD_PADDING = "PAYLOAD_PADDING"
private const val PAYLOAD_BACKGROUND = "PAYLOAD_BACKGROUND"

private const val PAYLOAD_IMAGE_SIZE = "PAYLOAD_IMAGE_SIZE"
private const val PAYLOAD_IMAGE_MARGIN = "PAYLOAD_IMAGE_MARGIN"
private const val PAYLOAD_IMAGE_PADDING = "PAYLOAD_IMAGE_PADDING"
private const val PAYLOAD_IMAGE_BACKGROUND = "PAYLOAD_IMAGE_BACKGROUND"