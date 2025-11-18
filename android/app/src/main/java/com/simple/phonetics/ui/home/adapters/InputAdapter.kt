package com.simple.phonetics.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.resize
import com.simple.phonetics.databinding.ItemHomeInputBinding

@ItemAdapter
class InputAdapter : ViewItemAdapter<InputViewItem, ItemHomeInputBinding>() {

    override val viewItemClass: Class<InputViewItem> by lazy {
        InputViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemHomeInputBinding {
        return ItemHomeInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemHomeInputBinding, viewType: Int, position: Int, item: InputViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if(payloads.contains("height"))binding.root.resize(height = item.height)
    }

    override fun onBindViewHolder(binding: ItemHomeInputBinding, viewType: Int, position: Int, item: InputViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.resize(height = item.height)
    }
}

data class InputViewItem(
    val id: String = "",
    val height: Int
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        height to "height"
    )
}