package com.simple.phonetics.data.repositories

import com.simple.ipa.dao.IpaProvider
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.dao.ipa.IpaOldProvider
import com.simple.phonetics.domain.repositories.IpaRepository
import kotlinx.coroutines.flow.Flow

class IpaRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val ipaProvider: IpaProvider,
    private val ipaOldProvider: IpaOldProvider
) : IpaRepository {

    private val api by lazy {
        apiProvider.api
    }

    private val ipaDaoNew by lazy {
        ipaProvider.ipaDao
    }

    private val ipaDaoOld by lazy {
        ipaOldProvider.ipaDao
    }

    override suspend fun syncIpa(languageCode: String): List<Ipa> {
        return api.syncIPA(languageCode = languageCode)
    }

    override suspend fun getAllOldAsync(languageCode: String): Flow<List<Ipa>> {
        return ipaDaoOld.getListAsync(languageCode = languageCode)
    }

    override suspend fun countAlOld(languageCode: String): Int {
        return ipaDaoOld.getCount(languageCode = languageCode)
    }

    override suspend fun getCount(languageCode: String): Int {
        return ipaDaoNew.getCount(languageCode = languageCode)
    }

    override suspend fun getCountAsync(languageCode: String): Flow<Int> {
        return ipaDaoNew.getCountAsync(languageCode = languageCode)
    }

    override suspend fun getIpaAsync(languageCode: String): Flow<List<Ipa>> {
        return ipaDaoNew.getListAsync(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(languageCode: String, list: List<Ipa>) {
        ipaDaoNew.insertOrUpdate(languageCode = languageCode, list = list)
    }

    override suspend fun deleteByKey(ipa: String, languageCode: String) {
        return ipaDaoNew.deleteByKey(ipa = ipa, languageCode = languageCode)
    }
}