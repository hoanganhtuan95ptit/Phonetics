package com.simple.phonetics.ui.home.view.event

import android.text.style.ForegroundColorSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.domain.usecase.event.GetCurrentEventAsyncUseCase
import com.simple.phonetics.domain.usecase.event.UpdateEventShowUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.state.ResultState
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EventHomeViewModel(
    private val updateEventShowUseCase: UpdateEventShowUseCase,
    private val getCurrentEventAsyncUseCase: GetCurrentEventAsyncUseCase,
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    val show: LiveData<Boolean> = MediatorLiveData()

    @VisibleForTesting
    val eventState: LiveData<ResultState<com.simple.phonetics.entities.Event>> = mediatorLiveData {

        getCurrentEventAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val historyList: LiveData<List<Sentence>> = mediatorLiveData {

        postDifferentValue(getPhoneticsHistoryAsyncUseCase.execute(null).firstOrNull().orEmpty())
    }

    @VisibleForTesting
    val eventInfo: LiveData<EventInfo> = combineSources(theme, translate, show, eventState, historyList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val show = show.value ?: return@combineSources
        val eventState = eventState.value ?: return@combineSources
        val historyList = historyList.value ?: return@combineSources

        if (show == false || historyList.isEmpty() || eventState !is ResultState.Success) {
            return@combineSources
        }

        val event = eventState.data

        EventInfo(
            event = event,

            image = event.image,

            title = translate[event.title].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnSurface)),
            message = translate[event.message].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnSurface)),

            positive = ButtonInfo(
                text = translate[event.positive].orEmpty()
                    .with(ForegroundColorSpan(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_16
                )
            ),
            negative = if (event.negative.isNotBlank()) ButtonInfo(
                text = translate[event.negative].orEmpty()
                    .with(ForegroundColorSpan(theme.colorOnSurfaceVariant)),
                background = Background(
                    backgroundColor = theme.colorBackground,
                    strokeColor = theme.colorOnSurfaceVariant,
                    strokeWidth = DP.DP_1,
                    cornerRadius = DP.DP_16
                )
            ) else {
                null
            },

            anchor = Background(
                backgroundColor = theme.colorBackground,
                cornerRadius = DP.DP_100,
            ),
            background = Background(
                backgroundColor = theme.colorBackground,
                cornerRadius_TL = DP.DP_16,
                cornerRadius_TR = DP.DP_16
            )
        ).apply {

            postDifferentValue(this)
        }
    }

    val eventInfoEvent: LiveData<Event<EventInfo>> = eventInfo.toEvent()

    fun show() {

        show.postDifferentValue(true)
    }

    fun updateShowEvent() = viewModelScope.launch(handler + Dispatchers.IO) {

        val event = eventState.value?.toSuccess()?.data ?: return@launch
        updateEventShowUseCase.execute(UpdateEventShowUseCase.Param(eventId = event.id))
    }

    data class EventInfo(
        val event: com.simple.phonetics.entities.Event,

        val image: String,

        val title: CharSequence = "",
        val message: CharSequence = "",

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null,

        val anchor: Background? = null,
        val background: Background? = null,
    )
}