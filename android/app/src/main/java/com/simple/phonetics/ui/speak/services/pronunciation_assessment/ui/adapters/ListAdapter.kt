package com.simple.phonetics.ui.speak.services.pronunciation_assessment.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.phonetics.databinding.ItemListBinding
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager

@ItemAdapter
class ListAdapter : ViewItemAdapter<ListViewItem, ItemListBinding>() {

    override val viewItemClass: Class<ListViewItem> by lazy {
        ListViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemListBinding {

        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val layoutManager = createFlexboxLayoutManager(context = binding.root.context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START
        binding.recyclerView.layoutManager = layoutManager

        return binding
    }

    override fun onBindViewHolder(binding: ItemListBinding, viewType: Int, position: Int, item: ListViewItem, payloads: MutableList<Any>) {

        binding.recyclerView.adapter.asObjectOrNull<MultiAdapter>()?.submitList(item.viewItemList)
        binding.recyclerView.setBackground(item.background)
    }

    override fun onBindViewHolder(binding: ItemListBinding, viewType: Int, position: Int, item: ListViewItem) {

        binding.recyclerView.adapter.asObjectOrNull<MultiAdapter>()?.submitList(item.viewItemList)
        binding.recyclerView.setBackground(item.background)
    }
}

data class ListViewItem(
    val id: String = "",

    val viewItemList: List<ViewItem>,

    val background: Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(id)

    override fun getContentsCompare(): List<Pair<Any, String>> = arrayListOf(
        viewItemList to "viewItemList"
    )
}