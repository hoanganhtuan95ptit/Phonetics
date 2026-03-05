package com.simple.feature.campaign.entities

import android.graphics.Color

data class Campaign(
    val deeplink: String? = null,

    val title: String? = null,
    val titleColor: Int = Color.TRANSPARENT,

    val message: String? = null,
    val messageColor: Int = Color.TRANSPARENT,

    val image: String? = null,
    val backgroundColor: Int = Color.TRANSPARENT
)
