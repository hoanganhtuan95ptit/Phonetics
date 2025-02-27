package com.simple.phonetics.entities

import androidx.annotation.Keep

@Keep
data class Language(
    val id: String = "",
    val name: String = "",
    val image: String = "",

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
        const val VI = "vi"
        const val ZH = "zh"
        const val KO = "ko"
        const val JA = "ja"
    }
}