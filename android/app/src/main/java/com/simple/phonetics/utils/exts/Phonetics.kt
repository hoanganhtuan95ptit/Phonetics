package com.simple.phonetics.utils.exts

import android.text.style.ForegroundColorSpan
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsViewItem
import com.simple.phonetics.ui.phonetics.adapters.SentenceViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.state.ResultState


fun Any.toViewItem(index: Int, total: Int, phoneticsCode: String, isShowDown: Boolean, isSupportTranslate: Boolean, theme: AppTheme, translate: Map<String, String>): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val item = this@toViewItem

    if (item is Phonetics) item.toViewItem(
        id = "${index * 1000}",
        isShowDown = isShowDown,
        phoneticsCode = phoneticsCode,
        theme = theme
    ).let {

        add(it)
        return@apply
    }


    if (item !is Sentence) {

        return@apply
    }


    item.phonetics.mapIndexed { indexPhonetic, phonetic ->

        phonetic.toViewItem(
            id = "${index * 1000 + indexPhonetic}",
            isShowDown = isShowDown,
            phoneticsCode = phoneticsCode,
            theme = theme
        )
    }.let {

        addAll(it)
    }

    if (isSupportTranslate) item.translateState.let { translateState ->

        val textPair = when (translateState) {

            is ResultState.Start -> {
                translate["translating"].orEmpty() to theme.colorOnSurface
            }

            is ResultState.Success -> {
                translateState.data to theme.colorOnSurface
            }

            else -> {
                translate["translate_failed"].orEmpty() to theme.colorError
            }
        }

        SentenceViewItem(
            id = "${index * 1000}",

            data = item,

            text = textPair.first.with(ForegroundColorSpan(textPair.second)),
            isLast = index == total
        )
    }.let {

        add(it)
    }
}

fun Phonetics.toViewItem(id: String, isShowDown: Boolean, phoneticsCode: String, theme: AppTheme): ViewItem {

    val codeAndIpa = this.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }

    val ipa = (codeAndIpa?.get(phoneticsCode) ?: codeAndIpa?.toList()?.first()?.second)?.firstOrNull().orEmpty()

    return PhoneticsViewItem(
        id = id,
        data = this,

        ipa = ipa.with(ForegroundColorSpan(if (this.ipa.size > 1) theme.colorPrimary else theme.colorError)),
        text = this.text.with(ForegroundColorSpan(theme.colorOnSurface)),

        isShowDown = isShowDown
    )
}
