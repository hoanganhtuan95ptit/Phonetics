package com.simple.phonetics.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemHistoryBinding
import com.tuanha.adapter.ViewItemAdapter
import com.tuanha.adapter.annotation.ItemAdapter
import com.tuanha.adapter.entities.ViewItem

@ItemAdapter
class HistoryAdapter(onItemClick: (View, HistoryViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<HistoryViewItem, ItemHistoryBinding>(onItemClick) {

    override val viewItemClass: Class<HistoryViewItem> by lazy {
        HistoryViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemHistoryBinding {
        return ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.TEXT)) refreshText(binding, item)
    }

    override fun onBindViewHolder(binding: ItemHistoryBinding, viewType: Int, position: Int, item: HistoryViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshText(binding, item)
    }

    private fun refreshText(binding: ItemHistoryBinding, item: HistoryViewItem) {
        binding.tvText.text = item.text
    }
}

data class HistoryViewItem(
    val id: String,

    val text: CharSequence = "",
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to Payload.TEXT,
    )
}