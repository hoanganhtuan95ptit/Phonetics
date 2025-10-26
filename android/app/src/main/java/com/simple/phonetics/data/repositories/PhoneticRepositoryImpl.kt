package com.simple.phonetics.data.repositories

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.simple.analytics.logAnalytics
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetic.dao.PhoneticProviderV2
import com.simple.phonetic.entities.Phonetic
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.R
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.PhoneticRoomDatabaseProvider
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.exts.wrapError
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doRunning
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext
import kotlin.math.min

class PhoneticRepositoryImpl(
    private val context: Context,

    private val appCache: AppCache,
    private val apiProvider: ApiProvider
) : PhoneticRepository {

    private val api by lazy {
        apiProvider.api
    }

    private val phoneticOldDao by lazy {
        GlobalContext.get().get<PhoneticRoomDatabaseProvider>().phoneticDao
    }

    private val phoneticNewDao by lazy {
        PhoneticProviderV2.phoneticDao
    }


    private val copyState = MutableLiveData<ResultState<Float>>()


    override suspend fun copy() {

        copyPhonetic().wrapError {

            ResultState.Failed(it)
        }.map {

            copyState.postValue(it)
            it
        }.filter { state ->

            state.doRunning {

                Log.d("tuanha", "copy: $it")
                if (it == 0f) logAnalytics("${PHONETIC_COPY}_start")
            }

            state.doSuccess {

                logAnalytics("${PHONETIC_COPY}_success")
            }

            state.doFailed {

                logAnalytics("${PHONETIC_COPY}_error")
                logCrashlytics("${PHONETIC_COPY}_error", it)
            }

            state.isCompleted()
        }.first()
    }

    override suspend fun copyStateAsync(): Flow<ResultState<Float>> {

        return copyState.asFlow()
    }

    override suspend fun syncPhonetic(language: Language, limit: Int): Flow<ResultState<Pair<Language.IpaSource, Float>>> {

        return syncPhonetic(limit = limit, sources = language.listIpa).map {

            it.doSuccess {

                appCache.setData("LANGUAGE_${language.id.uppercase()}_PHONETIC_UPDATE_DATE", System.currentTimeMillis())
            }

            it
        }
    }


    override suspend fun getLastTimeSyncPhonetic(language: Language): Long {

        return appCache.getData("LANGUAGE_${language.id.uppercase()}_PHONETIC_UPDATE_DATE", 0L)
    }


    override suspend fun getPhonetic(textList: List<String>): List<Phonetic> {

        return if (copyState.value.isSuccess()) {
            phoneticNewDao.getListBy(textList = textList.map { it.lowercase() })
        } else {
            logAnalytics("phonetic_old_used")
            phoneticOldDao.getListBy(textList = textList.map { it.lowercase() })
        }
    }

    override suspend fun getPhonetic(phoneticCode: String, textList: List<String>): List<Phonetic> {

        return if (copyState.value.isSuccess()) {
            phoneticNewDao.getListBy(ipaCode = phoneticCode.lowercase(), textList = textList.map { it.lowercase() })
        } else {
            logAnalytics("phonetic_old_used")
            phoneticOldDao.getListBy(phoneticCode = phoneticCode.lowercase(), textList = textList.map { it.lowercase() })
        }
    }

    override suspend fun getPhonetic(ipaQuery: String, phoneticCode: String, textList: List<String>): List<Phonetic> {

        return if (copyState.value.isSuccess()) {
            phoneticNewDao.getListBy(ipaQuery = ipaQuery, ipaCode = phoneticCode.lowercase(), textList = textList.map { it.lowercase() })
        } else {
            logAnalytics("phonetic_old_used")
            phoneticOldDao.getListBy(ipa = ipaQuery, phoneticCode = phoneticCode.lowercase(), textList = textList.map { it.lowercase() })
        }
    }


    override suspend fun suggest(text: String): List<Phonetic> {

        return if (copyState.value.isSuccess()) {
            phoneticNewDao.suggest(textQuery = text.lowercase())
        } else {
            logAnalytics("phonetic_old_used")
            phoneticOldDao.suggestPhonetics(text = text.lowercase())
        }
    }


    private fun copyPhonetic() = channelFlow {

        trySend(ResultState.Start)

        /**
         * kiểm tra db cũ có dữ liệu không
         */
        val phoneticOldCount = phoneticOldDao.countByText()
        if (phoneticOldCount < 100) {

            trySend(ResultState.Success(100f))
            awaitClose()
            return@channelFlow
        }


        /**
         * lấy giá trị đang đồng bộ
         */
        val appCache = GlobalContext.get().get<AppCache>()

        var offset: Int = if (BuildConfig.DEBUG) {
            0
        } else {
            appCache.getData(PHONETIC_COPY, 0)
        }
        if (offset > phoneticOldCount - 100) {

            trySend(ResultState.Success(100f))
            awaitClose()
            return@channelFlow
        }


        trySend(ResultState.Running(0f))


        /**
         * đồng bộ
         */
        val limit = 1_000

        while (true) {

            val batch = phoneticOldDao.allTextLimit(limit, offset)
            if (batch.isEmpty()) break

            batch.flatMap { item ->

                item.ipa.map {

                    Phonetic(
                        textWrap = item.text,
                        ipaCodeWrap = it.key,
                        ipaValueList = it.value
                    )
                }
            }.let {

                phoneticNewDao.insertOrUpdate(it)
            }

            delay(100)

            offset += batch.size

            appCache.getData(PHONETIC_COPY, offset)
            trySend(ResultState.Running(min(100f, offset * 100f / phoneticOldCount)))
        }


        trySend(ResultState.Success(100f))
        awaitClose()
    }


    private fun syncPhonetic(limit: Int = 10 * 1000, sources: List<Language.IpaSource>): Flow<ResultState<Pair<Language.IpaSource, Float>>> = channelFlow {

        sources.forEach { source ->

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

        awaitClose {

        }
    }

    private fun syncPhonetic(limit: Int = 10 * 1000, source: Language.IpaSource): Flow<ResultState<Float>> = channelFlow {

        /**
         * lấy dữ liệu source phonetic
         */
        val data = kotlin.runCatching {

            getSourcePhonetic(source)
        }.getOrElse {

            trySend(ResultState.Failed(it))
            awaitClose()
            return@channelFlow
        }


        /**
         * thêm vào db
         */
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

            val phoneticList = dataSplit.toPhonetics(source.code)

            phoneticNewDao.insertOrUpdate(phoneticList)

            count += dataSplit.length
            trySend(ResultState.Running(count * 1f / dataLength))
        }

        trySend(ResultState.Success(1f))

        awaitClose {

        }
    }

    private suspend fun getSourcePhonetic(it: Language.IpaSource): String {

        val result = runCatching {

            return api.syncPhonetics(it.source).string()
        }

        if (result.isFailure) if (it.code == Language.EN_UK) runCatching {

            return context.resources.openRawResource(R.raw.en_uk).bufferedReader().use { it.readText() }
        } else if (it.code == Language.EN_US) runCatching {

            return context.resources.openRawResource(R.raw.en_us).bufferedReader().use { it.readText() }
        }

        result.getOrElse { cause ->

            logCrashlytics("getSourcePhoneticError_${it.code}", cause)
        }

        throw result.exceptionOrNull() ?: RuntimeException("")
    }


    private fun String.toPhonetics(ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

        if (split.size < 2) return@mapNotNull null


        val text = split.removeAt(0)
        val ipaValue = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }

        Phonetic(
            textWrap = text,
            ipaCodeWrap = ipaCode,
            ipaValueList = ipaValue
        )
    }


    companion object {

        private const val PHONETIC_COPY = "phonetic_copy"
    }
}