package com.simple.phonetics.entities

import androidx.annotation.Keep

@Keep
data class Language(
    val id: String = "",
    val name: String = "",
    val image: String = "",

    val listIpa: List<Ipa> = emptyList(),
) {

    companion object {

        const val EN = "en"
        const val VI = "vi"
        const val ZH = "zh"
        const val KO = "ko"
        const val JA = "ja"
    }
}

@Keep
data class Ipa(
    val code: String = "",
    val name: String = "",
    val source: String = ""
)