package com.simple.phonetics.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.DEFAULT_LANGUAGE
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

class LanguageRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val appCache: AppCache
) : LanguageRepository {

    private val languageList = MediatorLiveData(DEFAULT_LANGUAGE)


    private val languageInput by lazy {

        val data = appCache.getData("language_input", "")

        MediatorLiveData(
            if (data.isBlank()) null else data.toObject<Language>()
        )
    }

    override suspend fun getPhoneticCodeSelected(): String {

        var phoneticCode = appCache.getData(PHONETIC_CODE, "")

        val languageInput = getLanguageInputAsync().first()

        if (languageInput.listIpa.firstOrNull { it.code == phoneticCode } != null) {

            return phoneticCode
        }

        val languageOutput = getLanguageOutputAsync().first()

        phoneticCode = languageInput.listIpa.first().code

        if (languageInput.id == Language.EN) if (languageOutput.country == "US") {

            phoneticCode = Language.EN_US
        } else if (languageOutput.country == "GB") {

            phoneticCode = Language.EN_UK
        }

        updatePhoneticCodeSelected(phoneticCode)

        return phoneticCode
    }

    override suspend fun getPhoneticCodeSelectedAsync(): Flow<String> = appCache.getDataAsync(PHONETIC_CODE).map {

        getPhoneticCodeSelected()
    }.distinctUntilChanged()

    override suspend fun updatePhoneticCodeSelected(code: String) {

        appCache.setData(PHONETIC_CODE, code)
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
            id = Locale.getDefault().language,
            name = Locale.getDefault().displayName,
            country = Locale.getDefault().country,
            image = "",
            listIpa = emptyList()
        )
    }

    override fun getLanguageOutputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageOutput())

        awaitClose()
    }

    override suspend fun getLanguageSupport(languageCode: String): List<Language> {

        val list = apiProvider.api.getLanguageSupport(languageCode = languageCode)

        languageList.postValue(list)

        return list
    }

    override suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>> {

        return languageList.asFlow()
    }

    companion object {

        private const val PHONETIC_CODE = "phonetic_code"
    }
}