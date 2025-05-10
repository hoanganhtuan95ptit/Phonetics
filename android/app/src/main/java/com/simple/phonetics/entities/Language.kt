package com.simple.phonetics.entities

import androidx.annotation.Keep

@Keep
data class Language(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    val country: String = "",

    val listIpa: List<IpaSource> = emptyList(),
) {

    @Keep
    data class IpaSource(
        val code: String = "",
        val name: String = "",
        val source: String = ""
    )

    companion object {

        const val EN = "en"
        const val EN_US = "US"
        const val EN_UK = "UK"
        const val VI = "vi"
        const val ZH = "zh"
        const val ES = "es"
        const val FR = "fr"
        const val AR = "ar"
        const val DE = "de"
        const val EO = "eo"
        const val FA = "fa"
        const val FI = "fi"
        const val IS = "is"
        const val JA = "ja"
        const val KM = "km"
        const val KO = "ko"
        const val MA = "ma"
        const val NB = "nb"
        const val NL = "nl"
        const val OR = "or"
        const val RO = "ro"
        const val SV = "sv"
        const val SW = "sw"
        const val TTS = "tts"
        const val JAM = "jam"
    }
}