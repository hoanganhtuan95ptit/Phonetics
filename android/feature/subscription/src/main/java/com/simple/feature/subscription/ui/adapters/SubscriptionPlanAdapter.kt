package com.simple.feature.subscription.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.feature.subscription.databinding.ItemSubscriptionPlanBinding
import com.simple.feature.subscription.entities.SubscriptionPlan
import com.simple.phonetics.Payload
import com.simple.phonetics.databinding.ItemLanguageBinding
import com.simple.phonetics.ui.language.adapters.LanguageViewItem

@ItemAdapter
class SubscriptionPlanAdapter(val onItemClick: (SubscriptionPlanViewItem) -> Unit = {}) : ViewItemAdapter<SubscriptionPlanViewItem, ItemSubscriptionPlanBinding>() {

    override val viewItemClass: Class<SubscriptionPlanViewItem> by lazy {
        SubscriptionPlanViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemSubscriptionPlanBinding {
        return ItemSubscriptionPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemSubscriptionPlanBinding> {
        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        viewHolder.binding.root.setDebouncedClickListener {

            val item = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            onItemClick(item)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemSubscriptionPlanBinding, viewType: Int, position: Int, item: SubscriptionPlanViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.NAME)) refreshName(binding, item)
        if (payloads.contains(Payload.THEME)) refreshTheme(binding, item)
    }

    override fun onBindViewHolder(binding: ItemSubscriptionPlanBinding, viewType: Int, position: Int, item: SubscriptionPlanViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshName(binding, item)
        refreshTheme(binding, item)
    }

    private fun refreshName(binding: ItemSubscriptionPlanBinding, item: SubscriptionPlanViewItem) {

        binding.tvTitle.setText(item.title)
        binding.tvDescription.setText(item.description)

        binding.tvPrice.setText(item.data.price)
    }

    private fun refreshTheme(binding: ItemSubscriptionPlanBinding, item: SubscriptionPlanViewItem) {

        binding.root.setBackground(item.background)
    }
}

data class SubscriptionPlanViewItem(
    val id: String,
    val data: SubscriptionPlan,

    val title: RichText,
    val description: RichText,

    val price: RichText,

    val background: Background,
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        data.id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        title to Payload.NAME,
        background to Payload.THEME,
    )
}
