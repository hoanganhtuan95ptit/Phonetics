package com.simple.phonetics.data.repositories

import com.simple.dao.entities.Ipa
import com.simple.dao.ipa.IpaDaoNew
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.ipa.IpaDao
import com.simple.phonetics.domain.repositories.IpaRepository
import kotlinx.coroutines.flow.Flow

class IpaRepositoryImpl(
    private val api: Api,
    private val ipaDaoOld: IpaDao,
    private val ipaDao: IpaDaoNew
) : IpaRepository {

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
        return ipaDao.getCount(languageCode = languageCode)
    }

    override suspend fun getCountAsync(languageCode: String): Flow<Int> {
        return ipaDao.getCountAsync(languageCode = languageCode)
    }

    override suspend fun getIpaAsync(languageCode: String): Flow<List<Ipa>> {
        return ipaDao.getListAsync(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(languageCode: String, list: List<Ipa>) {
        ipaDao.insertOrUpdate(languageCode = languageCode, list = list)
    }

    override suspend fun deleteByKey(ipa: String, languageCode: String) {
        return ipaDao.deleteByKey(ipa = ipa, languageCode = languageCode)
    }
}