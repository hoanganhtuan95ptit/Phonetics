package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Ipa

interface IpaRepository {

    suspend fun syncIpa(languageCode: String): List<Ipa>
}