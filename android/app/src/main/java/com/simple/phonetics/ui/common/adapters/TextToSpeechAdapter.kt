//package com.simple.phonetics.ui.common.adapters
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import com.simple.adapter.ViewItemAdapter
//import com.simple.adapter.annotation.ItemAdapter
//import com.simple.adapter.entities.ViewItem
//import com.simple.coreapp.utils.ext.RichText
//import com.simple.coreapp.utils.ext.emptyText
//import com.simple.coreapp.utils.ext.setText
//import com.simple.phonetics.databinding.ItemPhonetics2Binding
//
//
//@ItemAdapter
//class TextToSpeechAdapter(onItemClick: ((View, TextToSpeechViewItem) -> Unit)? = null) : ViewItemAdapter<TextToSpeechViewItem, ItemPhonetics2Binding>(onItemClick) {
//
//    override val viewItemClass: Class<TextToSpeechViewItem> by lazy {
//        TextToSpeechViewItem::class.java
//    }
//
//    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPhonetics2Binding {
//        return ItemPhonetics2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
//    }
//
//    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: TextToSpeechViewItem, payloads: MutableList<Any>) {
//        super.onBindViewHolder(binding, viewType, position, item, payloads)
//
//        binding.root.id = item.id
//        binding.root.text = item.text
//
//        if (payloads.contains("hasStroke")) hasStroke(binding, item, animate = true)
//        if (payloads.contains("textDisplay")) textDisplay(binding, item)
//        if (payloads.contains("strokeColor")) strokeColor(binding, item)
//    }
//
//    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: TextToSpeechViewItem) {
//        super.onBindViewHolder(binding, viewType, position, item)
//
//        binding.root.id = item.id
//        binding.root.text = item.text
//
//        hasStroke(binding, item)
//        textDisplay(binding, item)
//        strokeColor(binding, item)
//    }
//
//    private fun hasStroke(binding: ItemPhonetics2Binding, item: TextToSpeechViewItem, animate: Boolean = false) {
//        binding.root.hasStroke = item.hasStroke
//        binding.root.setLoading(false, item.hasStroke, animate)
//    }
//
//    private fun textDisplay(binding: ItemPhonetics2Binding, item: TextToSpeechViewItem) {
//        binding.root.tvPhonetic.setText(item.textDisplay)
//    }
//
//    private fun strokeColor(binding: ItemPhonetics2Binding, item: TextToSpeechViewItem) {
//        binding.root.strokeColor = item.strokeColor
//    }
//}
//
//data class TextToSpeechViewItem(
//    val id: String,
//    val text: String,
//) : ViewItem {
//
//    override fun areItemsTheSame(): List<Any> = listOf(
//        id
//    )
//
//    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
//    )
//}
