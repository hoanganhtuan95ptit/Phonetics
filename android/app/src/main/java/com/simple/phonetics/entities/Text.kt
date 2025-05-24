package com.simple.phonetics.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Text(
    val text: String,
    val type: Type
) : Parcelable {

    @Parcelize
    enum class Type : Parcelable {
        TEXT, IPA
    }
}
