package com.simple.phonetics.domain.usecase

import com.simple.coreapp.data.usecase.BaseUseCase
import com.simple.phonetics.data.dao.PhoneticsHistoryDao
import com.simple.phonetics.domain.entities.Sentence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPhoneticsHistoryAsyncUseCase(
    private val phoneticsHistoryDao: PhoneticsHistoryDao,
) : BaseUseCase<GetPhoneticsHistoryAsyncUseCase.Param, Flow<List<Sentence>>> {

    override suspend fun execute(param: Param?): Flow<List<Sentence>> {

        return phoneticsHistoryDao.getRoomListByAsync().map { list ->

            list.map { Sentence(it.text) }
        }
    }

    class Param() : BaseUseCase.Param()
}