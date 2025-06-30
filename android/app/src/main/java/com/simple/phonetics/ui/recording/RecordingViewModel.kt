package com.simple.phonetics.ui.recording

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.coreapp.utils.extentions.toPx
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.ui.base.adapters.ImageStateViewItem
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isStart
import com.unknown.size.uitls.exts.getOrZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordingViewModel(
    private val stopSpeakUseCase: StopSpeakUseCase,
    private val startSpeakUseCase: StartSpeakUseCase
) : BaseActionViewModel() {

    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(true)

    val speakState: LiveData<ResultState<String>> = MediatorLiveData()


    @VisibleForTesting
    val titleViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, translate, inputLanguage, outputLanguage, isReverse) {

        val theme = theme.get()
        val translate = translate.get()

        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageName = if (isReverse.value == true) {
            outputLanguage.name
        } else {
            inputLanguage.name
        }

        val list = arrayListOf<ViewItem>()

        ImageStateViewItem(
            id = ID.TITLE,

            anim = R.raw.anim_recording_title,

            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = 270.toPx()
            )
        ).let {

            list.add(it)
        }

        NoneTextViewItem(
            id = "TITLE",
            text = translate["recording_screen_title"].orEmpty()
                .replace("\$language_name", languageName)
                .with(ForegroundColor(theme.getOrTransparent("colorOnSurfaceVariant")))
                .with(languageName, Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            textStyle = TextStyle(
                textSize = 20f,
                textGravity = Gravity.CENTER
            ),
            padding = Padding(
                paddingVertical = DP.DP_24
            )
        ).let {

            list.add(it)
        }

        postValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(titleViewItemList, actionHeight) {

        val list = arrayListOf<ViewItem>()

        list.add(SpaceViewItem(id = "SPACE_PHONETICS_0", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        list.addAll(titleViewItemList.getOrEmpty())
        list.add(SpaceViewItem(id = "SPACE_PHONETICS_1", width = ViewGroup.LayoutParams.MATCH_PARENT, height = actionHeight.get()))

        postValueIfActive(list)
    }

    val actionInfo: LiveData<ImageStateViewItem> = listenerSourcesWithDiff(size, theme, titleViewItemList, speakState) {

        val size = size.value ?: return@listenerSourcesWithDiff
        val theme = theme.value ?: return@listenerSourcesWithDiff

        val speakState = speakState.value

        val width = (size.getOrZero("width") - 2 * DP.DP_24) / 3

        val viewItem = ImageStateViewItem(
            id = ID.SPEAK,

            anim = if (speakState.isRunning()) {
                R.raw.anim_recording
            } else {
                null
            },
            image = if (speakState.isRunning()) {
                null
            } else if (speakState == null || speakState.isStart() || speakState.isCompleted()) {
                R.drawable.ic_microphone_24dp
            } else {
                R.drawable.ic_microphone_slash_24dp
            },

            isLoading = speakState.isStart(),

            size = Size(
                width = width,
                height = DP.DP_70
            ),
            background = Background(
                strokeWidth = DP.DP_2,
                strokeColor = theme.getOrTransparent("colorPrimary"),
                cornerRadius = DP.DP_16
            ),

            imageSize = Size(
                width = DP.DP_60,
                height = DP.DP_60
            ),
            imagePadding = Padding(
                left = DP.DP_16,
                top = DP.DP_16,
                right = DP.DP_16,
                bottom = DP.DP_16
            )
        )

        postValueIfActive(viewItem)
    }

    fun updateReverse(it: Boolean) {

        isReverse.postValue(it)
    }

    fun startSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        val inputLanguage = inputLanguage.value ?: return@launch
        val outputLanguage = outputLanguage.value ?: return@launch

        val languageCode = if (isReverse.value == true) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        speakState.postValue(ResultState.Start)

        val param = StartSpeakUseCase.Param(
            languageCode = languageCode,
        )

        startSpeakUseCase.execute(param).launchCollect(viewModelScope) { state ->

            speakState.postValue(state)
        }
    }

    fun stopSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopSpeakUseCase.execute()
    }

    object ID {

        const val TITLE = "TITLE"

        const val SPEAK = "SPEAK"
    }
}