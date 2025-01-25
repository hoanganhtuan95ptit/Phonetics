package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Phonetics

interface PhoneticRepository {

    suspend fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetics>

    suspend fun getPhonetics(phonetics: List<String>): List<Phonetics>

    suspend fun insertOrUpdate(phonetics: List<Phonetics>)

    suspend fun getSourcePhonetic(it: Ipa): String
}