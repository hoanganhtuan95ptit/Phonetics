package com.simple.phonetics.ui.main.services.ads

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.Config.ADS_DEBUG
import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.appAds
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.get
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AdsViewModel(
    private val getConfigAsyncUseCase: GetConfigAsyncUseCase
) : BaseViewModel() {


    val lockAds = mutableSharedFlowWithDiff {

        emit(ConcurrentHashMap<String, Boolean>())
    }


    val config: LiveData<Map<String, String>> = mediatorLiveData {

        getConfigAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val isAdsEnable: Flow<Boolean> = combineSourcesWithDiff(config.asFlow(), lockAds) {

        emit(config.get()["ads_enable"] != "false" && lockAds.get().filter { it.value }.isEmpty())
    }

    val isAdsNativeEnable: Flow<Boolean> = combineSourcesWithDiff(config.asFlow()) {

        emit(config.get()["ads_native_enable"] != "false")
    }

    val isAdsNativeSpeakEnable: Flow<Boolean> = combineSourcesWithDiff(isAdsEnable, isAdsNativeEnable, config.asFlow()) {

        val isAdsNativeSpeakEnable = config.get()["ads_native_speak_enable"] != "false"

        emit(isAdsEnable.get() && isAdsNativeEnable.get() && isAdsNativeSpeakEnable)
    }

    @VisibleForTesting
    val request: LiveData<Long> = mediatorLiveData {

        var count = 0L

        appAds.collect {

            count++

            postValue(count)
        }
    }

    @VisibleForTesting
    val timeShow: LiveData<Long> = MediatorLiveData(0)

    val show: LiveData<Event<Boolean>> = combineSourcesWithDiff(config, request, timeShow, isAdsEnable.asLiveData()) {

        val config = config.get()
        val request = request.get()
        val timeShow = timeShow.get()
        val isAdsEnable = isAdsEnable.get()

        if (!isAdsEnable) {
            return@combineSourcesWithDiff
        }

        val spaceTime = config["ads_space_time_v2"]?.toLongOrNull() ?: if (ADS_DEBUG) 1000L else 30 * 60 * 1000L
        val spaceRequest = config["ads_space_request_v2"]?.toLongOrNull() ?: if (ADS_DEBUG) 3 else 5L

        if (System.currentTimeMillis() - timeShow <= spaceTime) {

            return@combineSourcesWithDiff
        }

        if (request % spaceRequest != 0L) {

            return@combineSourcesWithDiff
        }

        postValue(true.toEvent())
    }

    init {

        request.asFlow().launchCollect(viewModelScope) { count ->

            (1..20).forEach { if (count % it == 0L) logAnalytics("ads_show_count_$it") }
        }

        isAdsNativeSpeakEnable.launchIn(viewModelScope)
    }

    fun countShow() {

        timeShow.postValue(System.currentTimeMillis())
    }

    fun lockAds(tag: String, lock: Boolean) = viewModelScope.launch(handler) {

        lockAds.get()[tag] = lock
        lockAds.tryEmit(lockAds.get())
    }
}