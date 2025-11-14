package com.simple.phonetics.utils.exts

import android.util.Base64

fun String.fromBase64(): String {

    val decodedBytes = Base64.decode(this, Base64.DEFAULT)
    return String(decodedBytes, Charsets.UTF_8)
}

fun String.toBase64(): String {

    val encodedBytes = Base64.encode(this.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    return String(encodedBytes, Charsets.UTF_8)
}