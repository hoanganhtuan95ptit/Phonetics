package com.simple.phonetics.ui.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class PhoneticCodeAdapter constructor(onItemClick: (View, PhoneticCodeOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<PhoneticCodeOptionViewItem>(onItemClick)

data class PhoneticCodeOptionViewItem(
    override val id: String,
    override var data: String,

    override var text: String = "",

    override val isSelect: Boolean,

    override val textColor: Int,
    override val strokeColor: Int,
    override val backgroundColor: Int
) : OptionViewItem<String>(id, data, text, isSelect, textColor, strokeColor, backgroundColor) {

//    override fun refresh(isSelected: Boolean): OptionViewItem<String> = apply {
//        super.refresh(isSelected)
//
//        text = data.toText()
//    }
}
