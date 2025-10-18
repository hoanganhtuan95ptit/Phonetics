package com.simple.phonetics.domain.usecase.word

import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.word.entities.WordResourceCount
import kotlinx.coroutines.flow.Flow

class GetListWordResourceCountAsyncUseCase(
    private val wordRepository: WordRepository
) {

    suspend fun execute(param: Param): Flow<List<WordResourceCount>> {

        return wordRepository.getListWordResourceCountAsync(languageCode = param.languageCode)
    }

    data class Param(val languageCode: String)
}