package com.simple.phonetics.ui.phonetics.config.adapters

import android.view.View
import com.simple.coreapp.utils.extentions.Text
import com.simple.coreapp.utils.extentions.emptyText
import com.simple.coreapp.utils.extentions.toText
import com.simple.phonetics.domain.entities.PhoneticsCode
import com.simple.phonetics.ui.adapters.OptionAdapter
import com.simple.phonetics.ui.adapters.OptionViewItem

class PhoneticCodeAdapter constructor(onItemClick: (View, PhoneticCodeOptionViewItem) -> Unit = { _, _ -> }) : OptionAdapter<PhoneticCodeOptionViewItem>(onItemClick)

data class PhoneticCodeOptionViewItem(
    override val id: String, override var data: PhoneticsCode, override var text: Text<*> = emptyText(), override var isSelect: Boolean = false
) : OptionViewItem<PhoneticsCode>(id, data, text, isSelect) {

    override fun refresh(isSelected: Boolean): OptionViewItem<PhoneticsCode> = apply {
        super.refresh(isSelected)

        text = data.value.toText()
    }
}
