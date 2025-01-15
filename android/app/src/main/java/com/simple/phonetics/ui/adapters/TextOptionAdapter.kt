package com.simple.phonetics.ui.adapters

import android.view.View

class TextOptionAdapter constructor(onItemClick: (View, TextOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<TextOptionViewItem>(onItemClick)

data class TextOptionViewItem(
    override val id: String,

    override var text: String = "",

    override val isSelect: Boolean,

    override val textColor: Int,
    override val strokeColor: Int,
    override val backgroundColor: Int
) : OptionViewItem<String>(id, "", text, isSelect, textColor, strokeColor, backgroundColor) {

    override fun areItemsTheSame(): List<Any> = listOf(
        "TextOptionViewItem", id
    )
}
