package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic

interface PhoneticRepository {

    suspend fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetic>

    suspend fun getPhonetics(phonetics: List<String>): List<Phonetic>

    suspend fun getPhonetics(textList: List<String>, phoneticCode: String): List<Phonetic>

    suspend fun insertOrUpdate(phonetics: List<Phonetic>)

    suspend fun getSourcePhonetic(it: Language.IpaSource): String
}