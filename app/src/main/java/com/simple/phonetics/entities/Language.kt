package com.simple.phonetics.entities

data class Language(
    val id: String,
    val name: String,
    val image: String,

    val listIpa: List<Ipa>,

    val isSupportDetect: Boolean = false,
) {

    companion object {

        const val EN = "en"
        const val VI = "vi"
    }
}

data class Ipa(
    val code: String,
    val source: String
)