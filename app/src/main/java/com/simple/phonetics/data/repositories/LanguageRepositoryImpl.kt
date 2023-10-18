package com.simple.phonetics.data.repositories

import com.simple.coreapp.utils.extentions.offerActive
import com.simple.phonetics.domain.entities.Ipa
import com.simple.phonetics.domain.entities.Language
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.Locale

class LanguageRepositoryImpl : LanguageRepository {

    override fun getLanguageInput(): Language {

        return Language(
            LANGUAGE_DEFAULT,
            listOf(
                Ipa("UK", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"),
                Ipa("US", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt")
            )
        )
    }

    override fun getLanguageInputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageInput())

        awaitClose()
    }

    override fun getLanguageOutput(): Language {

        return Language(
            Locale.getDefault().language,
            emptyList()
        )
    }

    override fun getLanguageOutputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageOutput())

        awaitClose()
    }

    companion object {

        private const val LANGUAGE_DEFAULT = "en"
    }
}