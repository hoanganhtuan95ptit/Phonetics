package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.data.dao.HistoryDao
import com.simple.phonetics.entities.Sentence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPhoneticsHistoryAsyncUseCase(
    private val historyDao: HistoryDao,
) {

    suspend fun execute(param: Param?): Flow<List<Sentence>> {

        return historyDao.getRoomListByAsync(limit = param?.limit ?: 30).map { list ->

            list.map { Sentence(it.text) }
        }
    }

    data class Param(
        val limit: Int
    )
}