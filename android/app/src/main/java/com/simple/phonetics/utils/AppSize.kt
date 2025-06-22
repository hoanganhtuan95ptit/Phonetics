package com.simple.phonetics.utils

import com.unknown.size.uitls.exts.getOrZero

val Map<String, Int>.width: Int
    get() = getOrZero("width")

val Map<String, Int>.height: Int
    get() = getOrZero("height")