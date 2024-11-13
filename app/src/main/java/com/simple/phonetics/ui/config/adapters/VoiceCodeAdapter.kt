package com.simple.phonetics.ui.config.adapters

import android.view.View
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class VoiceCodeAdapter constructor(onItemClick: (View, VoiceOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<VoiceOptionViewItem>(onItemClick)

data class VoiceOptionViewItem(
    override val id: String,

    override var data: Int,
    override var text: String = "",
    override var isSelect: Boolean = false
) : OptionViewItem<Int>(id, data, text, isSelect) {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )
}
