package com.simple.phonetics.entities

import androidx.annotation.Keep

@Keep
data class Language(
    val id: String = "",
    val name: String = "",
    val image: String = "",

    val listIpa: List<Ipa> = emptyList(),

    val isSupportDetect: Boolean = false,
) {

    companion object {

        const val EN = "en"
        const val VI = "vi"
    }
}

@Keep
data class Ipa(
    val code: String = "",
    val source: String = ""
)