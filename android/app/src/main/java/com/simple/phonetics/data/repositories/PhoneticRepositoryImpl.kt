package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.PhoneticDao
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic
import com.simple.state.ResultState
import com.simple.state.isCompleted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class PhoneticRepositoryImpl(
    private val api: Api,
    private val appCache: AppCache,
    private val phoneticDao: PhoneticDao
) : PhoneticRepository {

    override suspend fun getSourcePhonetic(it: Language.IpaSource): String {

        return api.syncPhonetics(it.source).string()
    }

    override suspend fun syncPhonetic(language: Language, limit: Int): Flow<ResultState<Pair<Language.IpaSource, Float>>> = channelFlow {

        language.listIpa.forEach { source ->

            trySend(ResultState.Running(source to 0f))

            val state = syncPhonetic(source = source, limit = limit).filter {

                if (it is ResultState.Running) {

                    trySend(ResultState.Running(source to it.data))
                }

                it.isCompleted()
            }.first()

            if (state is ResultState.Success) {

                trySend(ResultState.Running(source to 1f))
            }

            if (state is ResultState.Failed) {

                trySend(state)
                awaitClose()
                return@channelFlow
            }
        }

        trySend(ResultState.Success(Language.IpaSource() to 1f))

        appCache.setData("LANGUAGE_${language.id.uppercase()}_PHONETIC_UPDATE_DATE", System.currentTimeMillis())

        awaitClose {

        }
    }

    override suspend fun getLastTimeSyncPhonetic(language: Language): Long {

        return appCache.getData("LANGUAGE_${language.id.uppercase()}_PHONETIC_UPDATE_DATE", 0L)
    }

    override suspend fun insertOrUpdate(phonetics: List<Phonetic>) {

        phoneticDao.insertOrUpdateEntities(phonetics)
    }


    override suspend fun getPhonetics(phonetics: List<String>): List<Phonetic> {

        return phoneticDao.getListBy(phonetics)
    }

    override suspend fun getPhonetics(textList: List<String>, phoneticCode: String): List<Phonetic> {

        return phoneticDao.getListBy(textList = textList, phoneticCode = phoneticCode)
    }

    override suspend fun getPhonetics(ipa: String, textList: List<String>, phoneticCode: String): List<Phonetic> {

        return phoneticDao.getListBy(ipa = ipa, textList = textList, phoneticCode = phoneticCode)
    }

    override suspend fun suggestPhonetics(text: String): List<Phonetic> {

        return phoneticDao.suggestPhonetics(text)
    }


    private fun syncPhonetic(limit: Int = 10 * 1000, source: Language.IpaSource): Flow<ResultState<Float>> = channelFlow {

        val data = kotlin.runCatching {

            getSourcePhonetic(source)
        }.getOrElse {

            trySend(ResultState.Failed(it))
            awaitClose()
            return@channelFlow
        }

        var count = 0
        val dataLength = data.length

        while (count < dataLength) {

            val start = count
            val end = if (dataLength < count + limit) {
                dataLength
            } else {
                count + limit
            }

            val dataSplit = data.substring(start, end)

            count += dataSplit.length

            val phoneticMap = toPhonetics(dataSplit, source.code)

            val phoneticsOldMap = getPhonetics(phoneticMap.keys.toList()).associateBy {
                it.text
            }

            phoneticMap.values.map { phonetic ->

                val ipaOld = phoneticsOldMap[phonetic.text]?.ipa ?: phonetic.ipa

                phonetic.ipa.putAll(ipaOld)

                phonetic
            }

            insertOrUpdate(phoneticMap.values.toList())

            trySend(ResultState.Running(count * 1f / dataLength))
        }

        trySend(ResultState.Success(1f))

        awaitClose {

        }
    }

    private fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetic> {

        val textAndPhonetic = hashMapOf<String, Phonetic>()

        dataSplit.toPhonetics(textAndPhonetic, code)

        return textAndPhonetic
    }

    private fun String.toPhonetics(textAndPhonetic: HashMap<String, Phonetic>, ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

        if (split.isEmpty()) return@mapNotNull null


        val text = split.removeAt(0)

        val ipa = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }


        val item = textAndPhonetic[text] ?: Phonetic(text).apply {

            textAndPhonetic[text] = this
        }

        if (item.ipa.isEmpty() || (!item.ipa.values.flatten().containsAll(ipa) && !ipa.containsAll(item.ipa.values.flatten()))) {

            item.ipa[ipaCode] = ipa
        }

        item
    }
}