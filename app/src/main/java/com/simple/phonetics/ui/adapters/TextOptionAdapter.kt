package com.simple.phonetics.ui.adapters

import android.view.View
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText

class TextOptionAdapter constructor(onItemClick: (View, TextOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<TextOptionViewItem>(onItemClick)

data class TextOptionViewItem(
    override val id: String,
    override var text: Text<*> = emptyText(),
    override var isSelect: Boolean = false
) : OptionViewItem<String>(id, "", text, isSelect) {

    override fun areItemsTheSame(): List<Any> = listOf(
        "TextOptionViewItem", id
    )
}
