package com.simple.phonetics.utils.exts

import android.view.Gravity
import android.view.ViewGroup
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.adapters.PhoneticsLoadingViewItem
import com.simple.phonetics.ui.base.adapters.PhoneticsViewItem
import com.simple.phonetics.ui.home.adapters.SentenceViewItem
import com.simple.state.ResultState

fun Phonetics(text: String, ipa: HashMap<String, List<String>>) = Phonetic(
    text = text
).apply {
    this.ipa = ipa
}

fun getPhoneticLoadingViewItem(theme: Map<String, Int>, background: Background? = null): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val backgroundWrap = background ?: Background(
        cornerRadius = DP.DP_100,
        backgroundColor = theme.getOrTransparent("colorLoading")
    )

    add(PhoneticsLoadingViewItem(id = "1", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "2", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "3", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "4", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "5", background = backgroundWrap))
}

fun Any.toViewItem(
    index: Int, total: Int, phoneticsCode: String,

    isShowSpeak: Boolean = true,

    isSupportSpeak: Boolean = false,
    isSupportListen: Boolean = false,
    isSupportTranslate: Boolean = false,

    theme: Map<String, Int>, translate: Map<String, String>
): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val item = this@toViewItem

    if (item is Phonetic) item.toViewItem(
        id = "${index * 1000}",

        isSupportSpeak = isSupportSpeak,
        isSupportListen = isSupportListen,

        phoneticsCode = phoneticsCode,

        theme = theme
    ).let {

        add(it)
        return@apply
    }


    if (item !is Sentence) {

        return@apply
    }

    if (isShowSpeak && isSupportSpeak && item.phonetics.size >= 2) {

        add(item.toSpeakViewItem(index, theme, translate))
    }

    item.phonetics.mapIndexed { indexPhonetic, phonetic ->

        phonetic.toViewItem(
            id = "${index * 1000 + indexPhonetic}",

            isSupportSpeak = isSupportSpeak,
            isSupportListen = isSupportListen,

            phoneticsCode = phoneticsCode,

            theme = theme
        )
    }.let {

        addAll(it)
    }

    if (isSupportTranslate) item.translateState.let { translateState ->

        val textPair = when (translateState) {

            is ResultState.Start -> {
                translate["translating"].orEmpty() to theme.getOrTransparent("colorOnSurface")
            }

            is ResultState.Success -> {
                translateState.data to theme.getOrTransparent("colorOnSurface")
            }

            else -> {
                translate["translate_failed"].orEmpty() to theme.getOrTransparent("colorError")
            }
        }

        SentenceViewItem(
            id = "${index * 1000}",

            data = item,

            text = textPair.first.with(ForegroundColor(textPair.second)),
            isLast = index == total
        )
    }.let {

        add(it)
    } else {

        add(SpaceViewItem(id = "$index", height = DP.DP_8))
    }
}

fun Sentence.toSpeakViewItem(index: Int, theme: Map<String, Int>, translate: Map<String, String>): ViewItem {

    return ClickTextViewItem(
        id = "${Id.SENTENCE}_${index}",
        data = this,

        size = Size(
            width = ViewGroup.LayoutParams.WRAP_CONTENT,
            height = DP.DP_40
        ),
        margin = Margin(
            left = DP.DP_4,
            right = DP.DP_12,
            top = DP.DP_12,
            bottom = DP.DP_12
        ),
        background = DEFAULT_BACKGROUND,

        text = translate["action_try_speak"].orEmpty().with(ForegroundColor(theme.getOrTransparent("colorPrimary"))),
        textStyle = TextStyle(
            textGravity = Gravity.CENTER_VERTICAL
        ),
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = DP.DP_40,
        ),
        textPadding = Padding(
            left = DP.DP_18 + DP.DP_8 * 2,
            top = DP.DP_8,
            right = DP.DP_8,
            bottom = DP.DP_8
        ),
        textBackground = Background(
            cornerRadius = DP.DP_16,
            strokeColor = theme.getOrTransparent("colorPrimary"),

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
            marginHorizontal = DP.DP_8
        ),
        imageLeftPadding = DEFAULT_PADDING
    )
}

fun Phonetic.toViewItem(
    id: String,

    isSupportSpeak: Boolean = false,
    isSupportListen: Boolean = false,

    phoneticsCode: String, theme: Map<String, Int>
): ViewItem {

    val codeAndIpa = this.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }

    val ipaList = (codeAndIpa?.get(phoneticsCode) ?: codeAndIpa?.toList()?.first()?.second).orEmpty()
    val text = ipaList.joinToString(separator = " - ")

    val image = if (isSupportSpeak) {
        R.drawable.img_down
    } else if (isSupportListen) {
        R.drawable.img_volume
    } else {
        0
    }

    return PhoneticsViewItem(
        id = id,
        data = this,

        ipa = text.with(ForegroundColor(if (ipaList.size > 1) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorError"))),
        text = this.text.with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),

        image = image,

        padding = Padding(
            top = DP.DP_12,
            bottom = DP.DP_12,
            left = DP.DP_4,
            right = DP.DP_12
        )
    )
}
