package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.dao.ipa.IpaDaoNew
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.dao.entities.Ipa
import kotlinx.coroutines.flow.Flow

class IpaRepositoryImpl(
    private val api: Api,
    private val ipaDao: IpaDaoNew
) : IpaRepository {

    override suspend fun syncIpa(languageCode: String): List<Ipa> {
        return api.syncIPA(languageCode = languageCode)
    }

    override suspend fun getIpaAsync(languageCode: String): Flow<List<Ipa>> {
        return ipaDao.getListAsync(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(languageCode: String, list: List<Ipa>) {
        ipaDao.insertOrUpdate(languageCode = languageCode, list = list)
    }

    override suspend fun getCount(languageCode: String): Int {
        return ipaDao.getCount(languageCode = languageCode)
    }

    override suspend fun getCountAsync(languageCode: String): Flow<Int> {
        return ipaDao.getCountAsync(languageCode = languageCode)
    }
}