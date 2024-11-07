package com.simple.phonetics.ui.adapters

import android.view.View

class TextOptionAdapter constructor(onItemClick: (View, TextOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<TextOptionViewItem>(onItemClick)

data class TextOptionViewItem(
    override val id: String,
    override var text: String = "",
    override var isSelect: Boolean = false
) : OptionViewItem<String>(id, "", text, isSelect) {

    override fun areItemsTheSame(): List<Any> = listOf(
        "TextOptionViewItem", id
    )
}
