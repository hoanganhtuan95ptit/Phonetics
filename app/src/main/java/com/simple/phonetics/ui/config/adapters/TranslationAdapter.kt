package com.simple.phonetics.ui.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class TranslationAdapter constructor(onItemClick: (View, TranslationOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<TranslationOptionViewItem>(onItemClick)

data class TranslationOptionViewItem(
    override val id: String,

    override var text: String = "",

    override val isSelect: Boolean,

    override val textColor: Int,
    override val strokeColor: Int,
    override val backgroundColor: Int
) : OptionViewItem<Boolean>(id, false, text, isSelect, textColor, strokeColor, backgroundColor) {

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
