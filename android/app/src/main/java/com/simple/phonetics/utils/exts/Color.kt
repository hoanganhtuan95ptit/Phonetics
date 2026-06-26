package com.simple.phonetics.utils.exts

fun Int.toPronunciationColor(): Int {
    return when (this.coerceIn(0, 100)) {
        in 90..100 -> 0xFF16A34A.toInt()
        in 75..89 -> 0xFF65A30D.toInt()
        in 60..74 -> 0xFFEAB308.toInt()
        in 40..59 -> 0xFFF97316.toInt()
        else -> 0xFFDC2626.toInt()
    }
}