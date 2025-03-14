package com.simple.phonetics.entities

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@Deprecated("remove")
@Keep
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyTranslate(
    val key: String,
    val value: String,
    val langCode: String,
)