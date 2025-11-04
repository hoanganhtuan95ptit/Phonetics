package com.simple.phonetics.ui.services.event

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.ImageViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.event.GetCurrentEventAsyncUseCase
import com.simple.phonetics.domain.usecase.event.UpdateEventShowUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.wrapLink
import com.simple.state.ResultState
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorBackground
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventViewModel(
    private val updateEventShowUseCase: UpdateEventShowUseCase,
    private val getCurrentEventAsyncUseCase: GetCurrentEventAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val eventState: LiveData<ResultState<com.simple.phonetics.entities.Event>> = mediatorLiveData {

        getCurrentEventAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, eventState) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val eventState = eventState.value ?: return@combineSourcesWithDiff

        val list = arrayListOf<ViewItem>()

        if (eventState !is ResultState.Success) {

            postValue(list)
            return@combineSourcesWithDiff
        }

        val event = eventState.data

        val title = translate.getOrKey(event.title)
        if (title.contains("title_", true)) {
            return@combineSourcesWithDiff
        }

        ImageViewItem(
            id = "1",
            image = event.image.wrapLink(),
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
            text = translate.getOrKey(event.title)
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
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
            text = translate.getOrKey(event.message)
                .with(ForegroundColor(theme.colorOnSurface)),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_MESSAGE", height = DP.DP_24))
        }

        postValue(list)
    }

    val eventInfo: LiveData<EventInfo> = combineSourcesWithDiff(theme, translate, eventState, viewItemList) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val eventState = eventState.value ?: return@combineSourcesWithDiff

        if (eventState !is ResultState.Success) {

            postValue(EventInfo(show = false))
            return@combineSourcesWithDiff
        }

        val event = eventState.data

        EventInfo(
            show = true,
            event = event,

            positive = ButtonInfo(
                text = translate.getOrKey(event.positive)
                    .with(ForegroundColor(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_16
                )
            ),
            negative = if (event.negative.isNotBlank()) ButtonInfo(
                text = translate.getOrKey(event.negative)
                    .with(ForegroundColor(theme.colorOnSurfaceVariant)),
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

            postValue(this)
        }
    }

    fun updateShowEvent() = viewModelScope.launch(handler + Dispatchers.IO) {

        val event = eventState.value?.toSuccess()?.data ?: return@launch
        updateEventShowUseCase.execute(UpdateEventShowUseCase.Param(eventId = event.id))
    }

    data class EventInfo(
        val show: Boolean,
        val event: com.simple.phonetics.entities.Event? = null,

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null,

        val viewItemList: List<ViewItem> = emptyList(),
    )
}