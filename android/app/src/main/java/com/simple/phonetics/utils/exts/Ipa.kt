package com.simple.phonetics.utils.exts

import com.simple.dao.entities.Ipa

fun Ipa.BackgroundColor(theme: Map<String, Int>) = if (ipa.contains("ː") && type == Ipa.Type.VOWELS_LONG.value) {
    theme.getOrTransparent("colorVowelsLong")
} else if (type == Ipa.Type.VOWELS_SHORT.value) {
    theme.getOrTransparent("colorVowelsShort")
} else if (type == Ipa.Type.CONSONANTS_VOICED.value) {
    theme.getOrTransparent("colorConsonantsVoiced")
} else if (type == Ipa.Type.CONSONANTS_UNVOICED.value) {
    theme.getOrTransparent("colorConsonantsUnvoiced")
} else {
    theme.getOrTransparent("colorDiphthongs")
}