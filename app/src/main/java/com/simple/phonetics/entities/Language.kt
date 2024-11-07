package com.simple.phonetics.entities

data class Language(
    val id: String,
    val name: String,

    val listIpa: List<Ipa>,
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