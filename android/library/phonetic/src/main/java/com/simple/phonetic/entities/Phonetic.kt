package com.simple.phonetic.entities

data class Phonetic(
    val text: String = "",
    val ipaCode: String = "",
    val ipaValue: String = ""
)

val Phonetic.ipaValueList: List<String>
    get() = ipaValue.split(",")

fun Phonetic(textWrap: String = "", ipaCodeWrap: String = "", ipaValueList: List<String> = emptyList()) = Phonetic(
    text = textWrap,
    ipaCode = ipaCodeWrap,
    ipaValue = ipaValueList.joinToString { it }
)