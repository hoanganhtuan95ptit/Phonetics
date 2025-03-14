package com.simple.phonetics.utils.exts

import com.simple.phonetics.entities.Language
import java.text.Normalizer


fun getLineDelimiters(): List<String> {

    return arrayListOf(".", "!", "?", "\n")
}

fun getWordDelimiters(languageCode: String): List<String> {

    val wordDelimiters = arrayListOf(" ", "\n", ":")
    if (languageCode in listOf(Language.ZH, Language.JA, Language.KO)) {

        wordDelimiters.add("")
    }

    return wordDelimiters
}

fun String.getWords(wordDelimiters: List<String>) = split(*wordDelimiters.toTypedArray())

fun String.normalize() = runCatching {

    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsWhitespace}\\p{Punct}\\p{So}]"), "")
}.getOrElse {

    this.lowercase()
}

// Regex để chỉ giữ lại chữ cái, số, khoảng trắng, và các ký tự Unicode khác
fun String.removeSpecialCharacters(): String = runCatching {

    replace(Regex("[^\\p{L}\\p{N}\\p{Z}\\p{So}]"), "")
}.getOrElse {

    this
}
