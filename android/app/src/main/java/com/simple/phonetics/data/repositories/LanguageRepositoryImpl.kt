package com.simple.phonetics.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.DEFAULT_LANGUAGE
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import java.util.Locale

class LanguageRepositoryImpl(
    private val api: Api,
    private val appCache: AppCache
) : LanguageRepository {

    private val languageList = MediatorLiveData(DEFAULT_LANGUAGE)


    private val languageInput by lazy {

        val data = appCache.getData("language_input", "")

        MediatorLiveData(
            if (data.isBlank()) null else data.toObject<Language>()
        )
    }

    override fun getLanguageInput(): Language? {

        return languageInput.value
    }

    override fun getLanguageInputAsync(): Flow<Language> {

        return languageInput.asFlow().filterNotNull()
    }

    override fun updateLanguageInput(language: Language) {

        appCache.setData("language_input", language.toJson())

        languageInput.postDifferentValue(language)
    }


    override fun getLanguageOutput(): Language {

        return Language(
            Locale.getDefault().language,
            Locale.getDefault().displayName,
            "",
            emptyList()
        )
    }

    override fun getLanguageOutputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageOutput())

        awaitClose()
    }

    override suspend fun syncLanguageSupport(languageCode: String): List<Language> {

        val list = api.getLanguageSupport(languageCode = languageCode)

        languageList.postValue(list)

        return list
    }

    override suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>> {

        return languageList.asFlow()
    }
}