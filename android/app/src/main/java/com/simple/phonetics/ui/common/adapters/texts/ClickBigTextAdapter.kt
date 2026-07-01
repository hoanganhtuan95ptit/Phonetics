package com.simple.phonetics.ui.common.adapters.texts

import android.view.View
import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.EventName
import com.simple.coreapp.databinding.ItemTextBinding
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.event.sendEvent
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class ClickBigTextAdapter(val onItemClick: ((View, ClickBigTextViewItem) -> Unit)? = null) : com.simple.adapter.ViewItemAdapter<ClickBigTextViewItem, ItemTextBinding>(), BigTextAdapter {

    override val viewItemClass: Class<ClickBigTextViewItem> by lazy {
        ClickBigTextViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemTextBinding {

        return createBinding(parent, viewType)
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemTextBinding> {

        val viewHolder = BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)

        val binding = viewHolder.binding

        binding.root.setDebouncedClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            onItemClick?.invoke(view, viewItem)
            sendEvent(EventName.TEXT_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }

    override fun onBindViewHolder(binding: ItemTextBinding, viewType: Int, position: Int, item: ClickBigTextViewItem, payloads: MutableList<Any>) {

        binding(binding, viewType, position, item as BigTextViewItem, payloads)
    }

    override fun onBindViewHolder(binding: ItemTextBinding, viewType: Int, position: Int, item: ClickBigTextViewItem) {

        binding(binding, viewType, position, item as BigTextViewItem)
    }
}

data class ClickBigTextViewItem(
    override val id: String = "",
    override val data: Any? = null,

    override val size: Size? = null,
    override val margin: Margin? = null,
    override val padding: Padding? = null,
    override var background: Background? = null,


    override var text: BigText = emptyText(),
    override var textStyle: TextStyle? = null,
    override val textSize: Size? = null,
    override val textMargin: Margin? = null,
    override val textPadding: Padding? = null,
    override var textBackground: Background? = null,


    override val imageLeft: Int? = null,
    override val imageLeftSize: Size? = null,
    override val imageLeftMargin: Margin? = null,
    override val imageLeftPadding: Padding? = null,
    override var imageLeftBackground: Background? = null,


    override val imageRight: Int? = null,
    override val imageRightSize: Size? = null,
    override val imageRightMargin: Margin? = null,
    override val imageRightPadding: Padding? = null,
    override var imageRightBackground: Background? = null,
) : BigTextViewItem()
