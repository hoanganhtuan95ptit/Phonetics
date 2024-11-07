package com.simple.phonetics.ui.phonetics.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class PhoneticCodeAdapter constructor(onItemClick: (View, PhoneticCodeOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<PhoneticCodeOptionViewItem>(onItemClick)

data class PhoneticCodeOptionViewItem(
    override val id: String,
    override var data: String,
    override var text: String = "",
    override var isSelect: Boolean = false
) : OptionViewItem<String>(id, data, text, isSelect) {

//    override fun refresh(isSelected: Boolean): OptionViewItem<String> = apply {
//        super.refresh(isSelected)
//
//        text = data.toText()
//    }
}
