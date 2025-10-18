package com.simple.phonetics.utils.exts

import com.simple.ipa.entities.Ipa

fun Ipa.BackgroundColor(theme: Map<String, Any>) = if (ipa.contains("ː") && type == Ipa.Type.VOWELS_LONG.value) {
    theme.colorVowelsLong
} else if (type == Ipa.Type.VOWELS_SHORT.value) {
    theme.colorVowelsShort
} else if (type == Ipa.Type.CONSONANTS_VOICED.value) {
    theme.colorConsonantsVoiced
} else if (type == Ipa.Type.CONSONANTS_UNVOICED.value) {
    theme.colorConsonantsUnvoiced
} else {
    theme.colorDiphthongs
}