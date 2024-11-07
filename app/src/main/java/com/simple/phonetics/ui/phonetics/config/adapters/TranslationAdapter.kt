package com.simple.phonetics.ui.phonetics.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class TranslationAdapter constructor(onItemClick: (View, TranslationOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<TranslationOptionViewItem>(onItemClick)

data class TranslationOptionViewItem(
    override val id: String,
    override var text: String = "",
    override var isSelect: Boolean = false
) : OptionViewItem<Boolean>(id, false, text, isSelect) {

//    override fun refresh(isSelected: Boolean): OptionViewItem<Boolean> = apply {
//        super.refresh(isSelected)
//
//        text = if (id.isNotBlank()) {
//
//            R.string.message_support_translate.toText()
//        } else {
//
//            R.string.message_translate_download.toText()
//        }
//    }
}
