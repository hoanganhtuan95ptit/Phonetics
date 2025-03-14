package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.dao.HistoryDao
import com.simple.phonetics.domain.repositories.HistoryRepository
import kotlinx.coroutines.flow.first

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override suspend fun get(limit: Int): List<String> {

        return historyDao.getRoomListByAsync(limit = limit).first().map {
            it.text
        }
    }
}