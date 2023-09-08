package com.simple.phonetics.ui.phonetics.config.adapters

import android.speech.tts.Voice
import android.view.View
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.toText
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class VoiceCodeAdapter constructor(onItemClick: (View, VoiceOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<VoiceOptionViewItem>(onItemClick)

data class VoiceOptionViewItem(
    override val id: String,

    override var data: Voice,
    override var text: Text<*> = emptyText(),
    override var isSelect: Boolean = false
) : OptionViewItem<Voice>(id, data, text, isSelect) {

    override fun refresh(isSelected: Boolean): OptionViewItem<Voice> = apply {
        super.refresh(isSelected)

        text = "Voice $id".toText()
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        "VoiceOptionViewItem", id
    )
}
