package com.simple.phonetics.ui.view.ads

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.Config.ADS_DEBUG
import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.appAds

class AdsViewModel(
    private val getConfigAsyncUseCase: GetConfigAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val config: LiveData<Map<String, String>> = mediatorLiveData {

        getConfigAsyncUseCase.execute().collect {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val request: LiveData<Long> = mediatorLiveData {

        appAds.collect {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val timeShow: LiveData<Long> = MediatorLiveData(0)

    val show: LiveData<Event<Boolean>> = combineSources(config, request, timeShow) {

        val config = config.get()
        val request = request.get()
        val timeShow = timeShow.get()

        val spaceTime = config["ads_space_time"]?.toLongOrNull() ?: if (ADS_DEBUG) 1000L else 10 * 60 * 1000L
        val spaceRequest = config["ads_space_request"]?.toLongOrNull() ?: if (ADS_DEBUG) 3 else 30L

        if (System.currentTimeMillis() - timeShow <= spaceTime) {

            return@combineSources
        }

        if (request % spaceRequest != 0L) {

            return@combineSources
        }

        postDifferentValue(true.toEvent())
    }

    fun countShow() {

        timeShow.postDifferentValue(System.currentTimeMillis())
    }
}