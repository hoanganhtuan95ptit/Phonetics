package com.simple.phonetics.ui.home.view.event

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.ImageViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.domain.usecase.event.GetCurrentEventAsyncUseCase
import com.simple.phonetics.domain.usecase.event.UpdateEventShowUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.state.ResultState
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventHomeViewModel(
    private val updateEventShowUseCase: UpdateEventShowUseCase,
    private val getCurrentEventAsyncUseCase: GetCurrentEventAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val eventState: LiveData<ResultState<com.simple.phonetics.entities.Event>> = mediatorLiveData {

        getCurrentEventAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, eventState) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val eventState = eventState.value ?: return@combineSources

        if (eventState !is ResultState.Success) {
            return@combineSources
        }

        val event = eventState.data

        val list = arrayListOf<ViewItem>()

        ImageViewItem(
            id = "1",
            image = event.image,
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_100 + DP.DP_70
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_IMAGE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "2",
            text = translate[event.title].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
            textStyle = TextStyle(
                textSize = 20f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "3",
            text = translate[event.message].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnSurface)),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_MESSAGE", height = DP.DP_24))
        }

        postDifferentValue(list)
    }

    @VisibleForTesting
    val eventInfo: LiveData<EventInfo> = combineSources(theme, translate, eventState, viewItemList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val eventState = eventState.value ?: return@combineSources

        if (eventState !is ResultState.Success) {
            return@combineSources
        }

        val event = eventState.data

        EventInfo(
            event = event,

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

            viewItemList = viewItemList.getOrEmpty()
        ).apply {

            postDifferentValue(this)
        }
    }
    val eventInfoEvent: LiveData<Event<EventInfo>> = eventInfo.toEvent()

    fun updateShowEvent() = viewModelScope.launch(handler + Dispatchers.IO) {

        val event = eventState.value?.toSuccess()?.data ?: return@launch
        updateEventShowUseCase.execute(UpdateEventShowUseCase.Param(eventId = event.id))
    }

    data class EventInfo(
        val event: com.simple.phonetics.entities.Event,

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null,

        val viewItemList: List<ViewItem>,
    )
}