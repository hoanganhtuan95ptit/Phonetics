package com.simple.phonetics.ui.speak

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.unknown.coroutines.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.R
import com.simple.phonetics.SpeakState
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.isSuccess
import com.simple.state.toRunning
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SpeakViewModel(
    private val stopSpeakUseCase: StopSpeakUseCase,
    private val startSpeakUseCase: StartSpeakUseCase,

    private val stopReadingUseCase: StopReadingUseCase,
    private val startReadingUseCase: StartReadingUseCase,

    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase
) : BaseActionViewModel() {

    val text: LiveData<String> = MediatorLiveData()


    val speakState: LiveData<ResultState<String>> = MediatorLiveData()

    val readingState: LiveData<ResultState<String>> = MediatorLiveData()


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Sentence>>> = combineSources(text, inputLanguage, outputLanguage, phoneticCodeSelected) {

        val param = GetPhoneticsAsyncUseCase.Param(
            textNew = text.get(),

            isReverse = false,
            saveToHistory = false,
            phoneticCode = phoneticCodeSelected.get(),
            inputLanguageCode = inputLanguage.get().id,
            outputLanguageCode = outputLanguage.get().id
        )

        getPhoneticsAsyncUseCase.execute(param).collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, translate, actionHeight, phoneticCodeSelected, phoneticsState, isSupportReading) {

        val theme = theme.get()
        val translate = translate.get()

        val state = phoneticsState.get()

        state.doStart {

            postValue(getPhoneticLoadingViewItem(theme = theme))
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        state.toSuccess()?.data.orEmpty().toViewItem(
            isSupportSpeak = false,
            isSupportListen = isSupportReading.value == true,
            isSupportTranslate = false,

            theme = theme,
            translate = translate
        ).let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.addAll(it)
            viewItemList.add(SpaceViewItem(id = "2", height = actionHeight.get()))
        }

        postValueIfActive(viewItemList)
    }

    val speakInfo: LiveData<SpeakInfo> = listenerSourcesWithDiff(size, theme, translate, isSupportSpeak, speakState) {

        val speakState = speakState.value

        val info = SpeakInfo(

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

            isShow = isSupportSpeak.value == true,
        )

        postValue(info)
    }

    val readingInfo: LiveData<ReadingInfo> = listenerSourcesWithDiff(size, theme, translate, isSupportReading, readingState) {

        val listenState = readingState.value

        val info = ReadingInfo(

            image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
                R.drawable.ic_volume_24dp
            } else {
                R.drawable.ic_pause_24dp
            },

            isShow = isSupportReading.value == true,
            isLoading = listenState.isStart()
        )

        postValue(info)
    }

    val copyInfo: LiveData<CopyInfo> = listenerSourcesWithDiff(size, theme, translate) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

        val info = CopyInfo(

            image = R.drawable.ic_copy_24dp,
            imageFilter = theme.colorPrimary,

            isShow = true,
            messageThankUser = emptyText()
        )

        postValue(info)
    }

    @VisibleForTesting
    val isCorrect: LiveData<Boolean> = combineSources(text, speakState) {

        val text = text.value ?: return@combineSources
        val speakState = speakState.value

        if (!speakState.isSuccess()) {

            return@combineSources
        }

        val speakResult = speakState?.toSuccess()?.data.orEmpty()

        val isCorrect = speakResult.equals(text, true)

        postValue(isCorrect)
    }
    val isCorrectEvent: LiveData<Event<Boolean>> = isCorrect.toEvent()


    val resultInfo: LiveData<ResultInfo> = listenerSourcesWithDiff(theme, translate, text, speakState) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

        val text = text.value ?: return@listenerSourcesWithDiff

        val speakState = speakState.value

        val speakResult = speakState?.toRunning()?.data
            ?: speakState?.toSuccess()?.data.orEmpty()

        if (speakResult in SpeakState.stateList) {

            return@listenerSourcesWithDiff
        }


        val isCorrect = speakResult.equals(text, true)

        val background = Background(
            strokeColor = if (isCorrect) theme.colorPrimary else theme.colorError,
            strokeWidth = DP.DP_1,
            cornerRadius = DP.DP_8,
            backgroundColor = if (isCorrect) theme.colorPrimaryVariant else theme.colorErrorVariant
        )

        val info = ResultInfo(
            result = speakResult
                .with(ForegroundColor(if (isCorrect) theme.colorOnPrimaryVariant else theme.colorOnErrorVariant)),
            isShow = speakResult.isNotBlank(),
            background = background
        )

        postValue(info)
    }


    fun updateText(it: String) {

        text.postValue(it)
    }

    fun startReading(text: String? = null) = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartReadingUseCase.Param(
            text = text ?: this@SpeakViewModel.text.value.orEmpty()
        )

        readingState.postValue(ResultState.Start)

        var job: Job? = null
        job = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

            readingState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopReading() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopReadingUseCase.execute()
    }


    fun startSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartSpeakUseCase.Param(
            languageCode = inputLanguage.value?.id ?: Language.EN,
        )

        speakState.postValue(ResultState.Start)

        startSpeakUseCase.execute(param).launchCollect(viewModelScope) { state ->

            speakState.postValue(state)
        }
    }

    fun stopSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopSpeakUseCase.execute()
    }


    data class ResultInfo(
        val result: RichText,

        val isShow: Boolean,

        val background: Background
    )

    data class CopyInfo(
        val image: Int,
        val imageFilter: Int = Color.TRANSPARENT,

        val isShow: Boolean,
        val messageThankUser: RichText,
    )

    data class SpeakInfo(
        val anim: Int?,
        val image: Int?,
        val isShow: Boolean,
        val isLoading: Boolean
    )

    data class ReadingInfo(
        val image: Int,
        val isShow: Boolean,
        val isLoading: Boolean
    )
}