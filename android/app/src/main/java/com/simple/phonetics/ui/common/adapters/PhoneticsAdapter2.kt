package com.simple.phonetics.ui.common.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.databinding.ItemPhonetics2Binding


@ItemAdapter
class PhoneticsAdapter2(onItemClick: ((View, PhoneticsViewItem2) -> Unit)? = null) : ViewItemAdapter<PhoneticsViewItem2, ItemPhonetics2Binding>(onItemClick) {

    override val viewItemClass: Class<PhoneticsViewItem2> by lazy {
        PhoneticsViewItem2::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPhonetics2Binding {
        return ItemPhonetics2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        binding.root.id = item.id
        binding.root.text = item.text

        if (payloads.contains("hasStroke")) hasStroke(binding, item, animate = true)
        if (payloads.contains("textDisplay")) textDisplay(binding, item)
        if (payloads.contains("strokeColor")) strokeColor(binding, item)
    }

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.id = item.id
        binding.root.text = item.text

        hasStroke(binding, item)
        textDisplay(binding, item)
        strokeColor(binding, item)
    }

    private fun hasStroke(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2, animate: Boolean = false) {
        binding.root.hasStroke = item.hasStroke
        binding.root.setLoading(false, item.hasStroke, animate)
    }

    private fun textDisplay(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2) {
        binding.root.tvPhonetic.setText(item.textDisplay)
    }

    private fun strokeColor(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2) {
        binding.root.strokeColor = item.strokeColor
    }
}

data class PhoneticsViewItem2(
    val id: String,
    val text: String,

    val textDisplay: RichText = emptyText(),

    val hasStroke: Boolean = false,
    val strokeColor: Int = Color.TRANSPARENT
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        textDisplay to "textDisplay",

        hasStroke to "hasStroke",
        strokeColor to "strokeColor"
    )
}
