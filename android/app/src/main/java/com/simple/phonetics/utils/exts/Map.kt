package com.simple.phonetics.utils.exts

import android.graphics.Color

fun Map<String, String>.getOrKey(key: String) = get(key) ?: key

fun Map<String, String>.getOrEmpty(key: String) = get(key) ?: ""

fun Map<String, Int>.getOrTransparent(key: String) = get(key) ?: Color.TRANSPARENT