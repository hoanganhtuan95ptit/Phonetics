package com.simple.dao.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ipa(
    val ipa: String = "",
    val examples: List<String> = arrayListOf(),

    val voice: String = "",
    val voices: Map<String, String> = emptyMap(),

    val type: String = Type.VOWELS_LONG.value,
) : Parcelable {


    enum class Type(val value: String) {

        VOWELS_LONG("vowels_long"),
        VOWELS_SHORT("vowels_short"),

        DIPHTHONGS("diphthongs"),

        CONSONANTS_VOICED("consonants_voiced"),
        CONSONANTS_UNVOICED("consonants_unvoiced")
    }
}