package com.simple.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Size
import com.simple.phonetics.databinding.ItemSpaceBinding

@ItemAdapter
class SpaceAdapter : ViewItemAdapter<SpaceViewItem, ItemSpaceBinding>() {

    override val viewItemClass: Class<SpaceViewItem> by lazy {
        SpaceViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemSpaceBinding {
        return ItemSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemSpaceBinding, viewType: Int, position: Int, item: SpaceViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.space.updateLayoutParams {

            width = item.width
            height = item.height
        }

        if (item.background == null) {

            binding.root.setBackgroundColor(Color.TRANSPARENT)
        } else {

            binding.root.setBackgroundResource(item.background)
        }
    }
}

fun SpaceViewItem(
    id: String = "",

    size: Size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.MATCH_PARENT,
    ),

    background: Int? = null
) = SpaceViewItem(
    id = id,
    width = size.width,
    height = size.height,
    background = background
)

data class SpaceViewItem(
    val id: String = "",

    val width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    val height: Int = ViewGroup.LayoutParams.MATCH_PARENT,

    val background: Int? = null
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )
}