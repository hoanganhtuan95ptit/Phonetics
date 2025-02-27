package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.entities.Ipa

class IpaRepositoryImpl(
    private val api: Api
) : IpaRepository {

    override suspend fun syncIpa(languageCode: String): List<Ipa> {
        return api.syncIPA(languageCode = languageCode)
    }
}