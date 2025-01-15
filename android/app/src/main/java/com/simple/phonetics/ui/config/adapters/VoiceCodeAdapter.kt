package com.simple.phonetics.ui.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class VoiceCodeAdapter constructor(onItemClick: (View, VoiceOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<VoiceOptionViewItem>(onItemClick)

data class VoiceOptionViewItem(
    override val id: String,

    override var data: Int,

    override var text: String = "",

    override val isSelect: Boolean,

    override val textColor: Int,
    override val strokeColor: Int,
    override val backgroundColor: Int
) : OptionViewItem<Int>(id, data, text, isSelect, textColor, strokeColor, backgroundColor) {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )
}
