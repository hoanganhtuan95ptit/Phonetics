package com.simple.phonetics.ui.common.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.ui.precompute.DrawSpec

abstract class PrecomputeAdapter<T : PrecomputeViewItem>(onItemClick: ((View, T) -> Unit)? = null) : ViewItemAdapter<T, ItemPrecomputeBinding>(onItemClick) {

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPrecomputeBinding {
        return ItemPrecomputeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    final override fun onBindViewHolder(binding: ItemPrecomputeBinding, viewType: Int, position: Int, item: T) {
        super.onBindViewHolder(binding, viewType, position, item)

        onBindViewHolder(binding, viewType, position, item, mutableListOf())
    }

    override fun onBindViewHolder(binding: ItemPrecomputeBinding, viewType: Int, position: Int, item: T, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        drawSpec(binding, item)
    }

    private fun drawSpec(binding: ItemPrecomputeBinding, item: T) {
        binding.root.spec = item.drawSpec
    }
}

abstract class PrecomputeViewItem : ViewItem {

    abstract val id: String
    abstract val maxWidth: Int
    abstract val drawSpec: DrawSpec

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )
}
