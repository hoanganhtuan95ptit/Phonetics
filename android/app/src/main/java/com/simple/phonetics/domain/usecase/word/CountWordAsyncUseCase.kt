package com.simple.phonetics.domain.usecase.word

import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import kotlinx.coroutines.flow.Flow

class CountWordAsyncUseCase(
    private val wordRepository: WordRepository
) {

    suspend fun execute(param: Param): Flow<Int> {

        return wordRepository.getCountAsync(resource = param.resource.value, languageCode = param.languageCode)
    }

    data class Param(val resource: Word.Resource, val languageCode: String)
}