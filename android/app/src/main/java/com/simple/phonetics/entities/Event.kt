package com.simple.phonetics.entities

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Event(
    val id: String = "",
    val name: String = "",

    val image: String = "",

    val timeEnd: String = "",
    val timeStart: String = "",

    val title: String = "",
    val message: String = "",

    val positive: String = "",
    val positiveDeepLink: String = "",

    val negative: String = "",
    val negativeDeepLink: String = ""
) : Parcelable