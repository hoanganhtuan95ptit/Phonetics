package com.simple.phonetics.utils.exts

import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.common.adapters.PhoneticsLoadingViewItem
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.ui.common.adapters.SpaceViewItem2
import com.simple.phonetics.ui.home.adapters.SentenceViewItem
import com.simple.phonetics.ui.home.adapters.TranslateViewItem
import com.simple.state.ResultState
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.with
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnSurface

fun getPhoneticLoadingViewItem(theme: Map<String, Any>, background: Background? = null): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val backgroundWrap = background ?: Background(
        cornerRadius = DP.DP_100,
        backgroundColor = theme.colorLoading
    )

    add(PhoneticsLoadingViewItem(id = "1", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "2", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "3", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "4", background = backgroundWrap))
    add(PhoneticsLoadingViewItem(id = "5", background = backgroundWrap))
}


fun List<Sentence>.toViewItem(
    isShowSpeak: Boolean = true,

    isSupportSpeak: Boolean = false,
    isSupportListen: Boolean = false,
    isSupportTranslate: Boolean = false,

    sizes: Map<String, Int>, theme: Map<String, Any>, translate: Map<String, String>
) = flatMapIndexed { indexItem: Int, item: Sentence ->

    item.toViewItem(
        index = indexItem,
        total = lastIndex,

        isShowSpeak = isShowSpeak,

        isSupportSpeak = isSupportSpeak,
        isSupportListen = isSupportListen,
        isSupportTranslate = isSupportTranslate,

        sizes = sizes,
        theme = theme,
        translate = translate
    )
}

private fun Sentence.toViewItem(
    index: Int, total: Int,

    isShowSpeak: Boolean = true,

    isSupportSpeak: Boolean = false,
    isSupportListen: Boolean = false,
    isSupportTranslate: Boolean = false,


    sizes: Map<String, Int>, theme: Map<String, Any>, translate: Map<String, String>
): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val item = this@toViewItem

    val maxWidth = sizes.width - 2 * 16.dp().toInt()


    val childViewItemList = arrayListOf<PrecomputeViewItem>()

    if (isShowSpeak && isSupportSpeak && item.phonetics.size >= 2) {

        childViewItemList.add(item.toSpeakViewItem(index, sizes, theme, translate))
    }


    item.phonetics.mapIndexed { indexPhonetic, phonetic ->

        phonetic.toViewItem(
            id = "${index * 1000 + indexPhonetic}",

            isSupportSpeaking = isSupportSpeak,
            isSupportReading = isSupportListen,

            sizes = sizes,
            themes = theme
        )
    }.let {

        childViewItemList.addAll(it)
    }

    if (childViewItemList.isNotEmpty()) SentenceViewItem(
        id = "${index * 1000}",
        maxWidth = maxWidth,

        data = item,

        viewItems = childViewItemList
    ).let {

        add(it)
    }

    if (isSupportTranslate) item.translateState.let { translateState ->

        val text = when (translateState) {

            is ResultState.Start -> {
                translate.getOrEmpty("translating")
                    .withStyleBodyLarge()
                    .with(BigForegroundColor(theme.colorOnSurface))
            }

            is ResultState.Success -> {
                translateState.data
                    .withStyleBodyLarge()
                    .with(BigForegroundColor(theme.colorOnSurface))
            }

            else -> {
                translate.getOrEmpty("translate_failed")
                    .withStyleBodyLarge()
                    .with(BigForegroundColor(theme.colorError))
            }
        }

        TranslateViewItem(
            id = "${index * 1000}",
            maxWidth = maxWidth,

            data = item,

            text = text
                .build(),

            isLast = index == total,
        )
    }.let {

        add(it)
    } else {

        add(SpaceViewItem2(id = "1", maxWidth = sizes.width - 2 * 16.dp().toInt(), height = 8.dp()))
    }
}
