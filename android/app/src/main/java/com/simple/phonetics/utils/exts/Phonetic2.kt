package com.simple.phonetics.utils.exts

import android.graphics.Color
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.common.adapters.PhoneticsViewItem2
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.ui.home.adapters.SpeakSentenceViewItem
import com.simple.ui.precompute.image.ColorFilter
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigRoundedBackground
import com.simple.ui.precompute.text.with
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary

internal fun Sentence.toSpeakViewItem(
    index: Int,
    sizes: Map<String, Int>,
    themes: Map<String, Any>,
    strings: Map<String, String>
): PrecomputeViewItem {

    return SpeakSentenceViewItem(
        id = "${Id.SENTENCE}_${index}",
        maxWidth = sizes.width,

        sentence = this,

        text = strings.getOrEmpty("action_try_speak")
            .withStyleBodyLarge()
            .with(BigForegroundColor(themes.colorPrimary))
            .build(),

        contentColor = themes.colorPrimary
    )
}

internal fun com.simple.phonetic.entities.Phonetic.toViewItem(
    id: String,

    isSupportReading: Boolean = false,
    isSupportSpeaking: Boolean = false,

    sizes: Map<String, Int>,
    themes: Map<String, Any>,
): PrecomputeViewItem {

    val icon = if (isSupportSpeaking) {
        R.drawable.img_down
    } else if (isSupportReading) {
        R.drawable.img_volume
    } else {
        0
    }

    val iconDisplay = icon.toBuilder()
        .addTransform(ColorFilter(themes.colorOnSurface))
        .build()

    val textDisplay = text
        .withStyleBodyLarge()
        .with(BigBold, BigForegroundColor(themes.colorOnSurface))


    val ipaList = ipaValueList
    val ipa = ipaList.joinToString(separator = " - ")

    val phoneticDisplay = ipa
        .withStyleBodyLarge()
        .with(BigForegroundColor(if (ipaList.size > 1) themes.colorPrimary else themes.colorError))


    return PhoneticsViewItem2(
        id = "$id PhoneticsViewItem",
        maxWidth = sizes.width,

        text = text,

        textDisplay = textDisplay.build(),
        phoneticDisplay = phoneticDisplay.build(),

        iconShow = isSupportReading || isSupportSpeaking,
        iconDisplay = iconDisplay,

        onlyReading = isSupportReading && !isSupportSpeaking,

        strokeColor = themes.colorPrimary,
    )
}