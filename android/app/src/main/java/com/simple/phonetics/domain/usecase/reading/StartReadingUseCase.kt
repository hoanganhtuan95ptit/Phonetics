package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.GlobalContext

class StartReadingUseCase(
    private val readingRepository: ReadingRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<String>> {

        return readingRepository.startReading(
            text = param.text,

            phoneticCode = languageRepository.getPhoneticCodeSelected(),

            voiceId = readingRepository.getVoiceIdSelected(),
            voiceSpeed = readingRepository.getVoiceSpeed()
        )
    }

    data class Param(
        val text: String
    )

    companion object {

        val install: StartReadingUseCase by lazy {
            GlobalContext.get().get()
        }
    }
}