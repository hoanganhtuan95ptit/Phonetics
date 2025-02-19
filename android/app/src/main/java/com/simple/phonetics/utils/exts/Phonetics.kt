package com.simple.phonetics.utils.exts

import android.text.style.ForegroundColorSpan
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsViewItem
import com.simple.phonetics.ui.phonetics.adapters.SentenceViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.state.ResultState


fun Any.toViewItem(
    index: Int, total: Int, phoneticsCode: String,

    isShowSpeak: Boolean = false,
    isShowListen: Boolean = false,
    isSupportTranslate: Boolean = false,

    theme: AppTheme, translate: Map<String, String>
): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val item = this@toViewItem

    if (item is Phonetics) item.toViewItem(
        id = "${index * 1000}",
        isShowSpeak = isShowSpeak,
        isShowListen = isShowListen,
        phoneticsCode = phoneticsCode,
        theme = theme
    ).let {

        add(it)
        return@apply
    }


    if (item !is Sentence) {

        return@apply
    }

    if (isShowSpeak && item.phonetics.size >= 2) {

        add(item.toSpeakViewItem(index, theme, translate))
    }

    item.phonetics.mapIndexed { indexPhonetic, phonetic ->

        phonetic.toViewItem(
            id = "${index * 1000 + indexPhonetic}",
            isShowSpeak = isShowSpeak,
            isShowListen = isShowListen,
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
    } else {

        add(SpaceViewItem(id = "$index", height = DP.DP_8))
    }
}

fun Sentence.toSpeakViewItem(index: Int, theme: AppTheme, translate: Map<String, String>): ViewItem {

    return ClickTextViewItem(
        id = "${Id.SENTENCE}_${index}",
        data = this,

        margin = Margin(
            top = DP.DP_16,
            bottom = DP.DP_8,
            right = DP.DP_16
        ),

        text = translate["action_try_speak"].orEmpty().with(ForegroundColorSpan(theme.colorPrimary)),

        textPadding = Padding(
            left = DP.DP_18 + DP.DP_8 * 2,
            top = DP.DP_8,
            right = DP.DP_8,
            bottom = DP.DP_8
        ),
        textBackground = Background(
            cornerRadius = DP.DP_16,
            strokeColor = theme.colorPrimary,

            strokeWidth = DP.DP_2,
            strokeDashGap = DP.DP_4,
            strokeDashWidth = DP.DP_4
        ),

        imageLeft = R.drawable.ic_microphone_24dp,

        imageLeftSize = Size(
            width = DP.DP_18,
            height = DP.DP_40
        ),
        imageLeftMargin = Margin(
            left = DP.DP_10,
            right = DP.DP_8,
        )
    )
}

fun Phonetics.toViewItem(
    id: String,
    isShowSpeak: Boolean = false,
    isShowListen: Boolean = false,
    phoneticsCode: String, theme: AppTheme
): ViewItem {

    val codeAndIpa = this.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }

    val ipaList = (codeAndIpa?.get(phoneticsCode) ?: codeAndIpa?.toList()?.first()?.second).orEmpty()
    val text = ipaList.joinToString(separator = " - ")

    val image = if (isShowSpeak) {
        R.drawable.img_down
    } else if (isShowListen) {
        R.drawable.img_volume
    } else {
        0
    }

    return PhoneticsViewItem(
        id = id,
        data = this,

        ipa = text.with(ForegroundColorSpan(if (ipaList.size > 1) theme.colorPrimary else theme.colorError)),
        text = this.text.with(ForegroundColorSpan(theme.colorOnSurface)),

        image = image
    )
}
