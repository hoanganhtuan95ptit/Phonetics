package com.simple.phonetics.utils.exts

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.utils.AppTheme

fun Ipa.BackgroundColor(theme: AppTheme) = if (ipa.contains("ː") && type == Ipa.Type.VOWELS_LONG.value) {
    theme.vowelsLong
} else if (type == Ipa.Type.VOWELS_SHORT.value) {
    theme.vowelsShort
} else if (type == Ipa.Type.CONSONANTS_VOICED.value) {
    theme.consonantsVoiced
} else if (type == Ipa.Type.CONSONANTS_UNVOICED.value) {
    theme.consonantsUnvoiced
} else {
    theme.diphthongs
}