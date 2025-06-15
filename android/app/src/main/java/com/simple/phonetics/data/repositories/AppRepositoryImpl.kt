package com.simple.phonetics.data.repositories

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.detect.data.tasks.DetectStateTask
import com.simple.detect.data.tasks.DetectTask
import com.simple.detect.entities.DetectOption
import com.simple.detect.entities.DetectProvider
import com.simple.detect.entities.DetectState
import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.Module
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.KeyTranslateDao
import com.simple.phonetics.data.dao.translate.TranslateDao
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.toSuccess
import com.simple.task.executeAsyncAll
import com.simple.task.executeAsyncByPriority
import com.simple.task.executeSyncByPriority
import com.simple.translate.data.tasks.TranslateTask
import com.simple.translate.entities.TranslateRequest
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext

class AppRepositoryImpl(
    private val context: Context,

    private val api: Api,
    private val appCache: AppCache,
    private val translateDao: TranslateDao,
    private val keyTranslateDao: KeyTranslateDao
) : AppRepository {

    private val events: LiveData<List<Event>> = MutableLiveData()

    private val configs: LiveData<Map<String, String>> = MutableLiveData()


    private val job by lazy {
        JobQueue()
    }

    private val splitInstallManager by lazy {
        SplitInstallManagerFactory.create(context)
    }


    override suspend fun getCountTranslateOld(): Int {
        return keyTranslateDao.count()
    }

    override suspend fun getAllTranslateOld(): List<KeyTranslate> {
        return keyTranslateDao.getAll()
    }


    override suspend fun syncTranslate(languageCode: String): Map<String, String> {
        return api.syncTranslate(languageCode = languageCode)
    }

    override suspend fun updateTranslate(languageCode: String, map: Map<String, String>) {
        translateDao.insertOrUpdate(languageCode = languageCode, map = map)
    }

    override suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>> {
        return translateDao.getAllAsync(languageCode = languageCode)
    }

    override suspend fun getCountTranslate(): Int {
        return translateDao.getCount()
    }

    override suspend fun getKeyTranslateDefault(): Map<String, String> {
        return DEFAULT_TRANSLATE
    }


    @Suppress("UNCHECKED_CAST")
    override suspend fun initModuleSync(pair: Pair<String, String>): ResultState<Unit> {

        val state = downloadModuleQueue(pair.first)

        if (state is ResultState.Failed) {

            return state
        }

        return try {

            AppInitializer.getInstance(context).initializeComponent(Class.forName(pair.second) as Class<Initializer<Any>>)

            ResultState.Success(Unit)
        } catch (e: Exception) {

            ResultState.Failed(e)
        }
    }

    private suspend fun downloadModuleQueue(moduleName: String) = channelFlow {

        job.submit(this.coroutineContext) {

            val state = downloadModuleSync(moduleName)
            trySend(state)
        }

        awaitClose {

        }
    }.first()

    private suspend fun downloadModuleSync(moduleName: String) = channelFlow {

        if (splitInstallManager.installedModules.contains(moduleName)) {

            trySend(ResultState.Success(Unit))
            awaitClose()
            return@channelFlow
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        val listener = SplitInstallStateUpdatedListener { state ->


            Log.d("tuanha", "downloadModuleSync: $moduleName ${state.status()}")
            when (state.status()) {

                SplitInstallSessionStatus.INSTALLED -> {

                    trySend(ResultState.Success(Unit))
                }

                SplitInstallSessionStatus.FAILED -> {

                    trySend(ResultState.Failed(RuntimeException("")))
                }

                else -> {

                }
            }
        }

        splitInstallManager.registerListener(listener)

        splitInstallManager.startInstall(request).addOnCompleteListener {

            Log.d("tuanha", "addOnCompleteListener: $moduleName")
        }.addOnFailureListener {

            Log.d("tuanha", "addOnFailureListener: $moduleName", it)
            trySend(ResultState.Failed(it))
        }

        awaitClose {
            splitInstallManager.unregisterListener(listener)
        }
    }.first()

    override suspend fun detect(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String> {

        val state = initModuleSync(Module.MLKIT)

        if (state is ResultState.Failed) {

            return state
        }

        val detectState = GlobalContext.get().getAll<DetectTask>().executeAsyncByPriority(DetectTask.Param(path, languageCodeInput, languageCodeOutput, DetectOption.TEXT, 500))

        if (detectState is ResultState.Failed) {

            return detectState
        }

        detectState.toSuccess()?.data?.joinToString("\n") { it.text }.orEmpty().let {

            return ResultState.Success(it)
        }
    }

    override suspend fun checkDetect(languageCodeInput: String, languageCodeOutput: String): Boolean {

        val state = initModuleSync(Module.MLKIT)

        if (state is ResultState.Failed) {

            return false
        }

        val detectState = GlobalContext.get().getAll<DetectStateTask>().executeAsyncAll(DetectStateTask.Param(languageCode = languageCodeInput)).firstOrNull()

        val detectStateList = detectState?.toSuccess()?.data?.filterIsInstance<ResultState.Success<Pair<DetectProvider, DetectState>>>()?.map {

            it.data
        }

        return detectStateList?.any { it.second == DetectState.READY } == true
    }

    override suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>> {

        val state = initModuleSync(Module.MLKIT)

        if (state is ResultState.Failed) {

            return state
        }

        val input = text.map {

            TranslateRequest(
                text = it,
                languageCode = languageCodeInput
            )
        }

        val translateState = GlobalContext.get().getAll<TranslateTask>().executeSyncByPriority(TranslateTask.Param(input = input, outputCode = languageCodeOutput))

        return translateState
    }

    override suspend fun checkTranslate(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean> {

        val state = translate(languageCodeInput = languageCodeInput, languageCodeOutput = languageCodeOutput, "hello")

        state.toSuccess()?.data?.firstOrNull()?.translateState?.doSuccess {

            return ResultState.Success(true)
        }

        state.doFailed {

            return ResultState.Failed(it)
        }

        return ResultState.Failed(RuntimeException(""))
    }


    override suspend fun syncConfigs(): Map<String, String> {
        return api.syncConfig()
    }

    override suspend fun getConfigsAsync(): Flow<Map<String, String>> {
        return configs.asFlow()
    }

    override suspend fun updateConfigs(map: Map<String, String>) {
        configs.postDifferentValue(map)
    }


    override fun getEventIdShow(): String? {
        return appCache.getData("EVENT_ID_SHOW", "")
    }

    override fun updateEventIdShow(id: String) {
        appCache.setData("EVENT_ID_SHOW", id)
    }

    override suspend fun syncEvents(languageCode: String): List<Event> {
        return api.syncEvent(languageCode = languageCode)
    }

    override suspend fun getEventsAsync(): Flow<List<Event>> {
        return events.asFlow()
    }

    override suspend fun updateEvents(list: List<Event>) {
        events.postValue(list)
    }


    override suspend fun getTranslateSelected(): String {

        return appCache.getData(TRANSLATE_STATUS, "0")
    }

    override suspend fun getTranslateSelectedAsync(): Flow<String> {

        return appCache.getDataAsync(TRANSLATE_STATUS).map {

            getTranslateSelected()
        }.distinctUntilChanged()
    }

    override suspend fun updateTranslateSelected(translateSelected: String) {

        appCache.setData(TRANSLATE_STATUS, translateSelected)
    }


    override fun <T> updateData(key: String, value: T) {

        appCache.setData(key, value)
    }

    override fun <T> getData(key: String, default: T): T {

        return appCache.getData(key, default)
    }

    override fun <T> getDataAsync(key: String, default: T): Flow<T> {

        return appCache.getDataAsync(key, default)
    }

    companion object {

        private const val TRANSLATE_STATUS = "translate_status"
    }
}