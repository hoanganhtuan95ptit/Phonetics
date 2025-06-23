package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.dao.PhoneticRoomDatabaseProvider
import com.simple.phonetics.domain.repositories.HistoryRepository
import kotlinx.coroutines.flow.first

class HistoryRepositoryImpl(
    private val phoneticRoomDatabaseProvider: PhoneticRoomDatabaseProvider
) : HistoryRepository {

    private val historyDao by lazy {
        phoneticRoomDatabaseProvider.historyDao
    }

    override suspend fun get(limit: Int): List<String> {

        return historyDao.getRoomListByAsync(limit = limit).first().map {
            it.text
        }
    }
}