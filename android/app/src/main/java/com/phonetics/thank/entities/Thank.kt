package com.phonetics.thank.entities

data class Thank(

    val id: String? = null,
    val image: String? = null,

    val title: String? = null,
    val message: String? = null,

    val author: String? = null,

    val negative: String? = null,
    val negativeDeeplink: String? = null,
    val positive: String? = null,
    val positiveDeeplink: String? = null
)
