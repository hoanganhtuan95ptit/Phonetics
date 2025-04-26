package com.simple.phonetics.ui.speak

import android.text.style.ForegroundColorSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
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
import com.simple.state.toSuccess
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

    val isSupportSpeak: LiveData<Boolean> = MediatorLiveData(true)


    val listenState: LiveData<ResultState<String>> = MediatorLiveData()

    val isSupportListen: LiveData<Boolean> = MediatorLiveData(true)


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(text, inputLanguage, outputLanguage, phoneticCodeSelected) {

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

    val viewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, actionHeight, phoneticCodeSelected, phoneticsState, isSupportListen) {

        val theme = theme.get()
        val translate = translate.get()

        val state = phoneticsState.get()
        val phoneticsCode = phoneticCodeSelected.get()

        state.doStart {

            postDifferentValue(getPhoneticLoadingViewItem(theme = theme))
            return@combineSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        val listItem = state.toSuccess()?.data.orEmpty()

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,

                isSupportSpeak = false,
                isSupportListen = isSupportListen.value == true,
                isSupportTranslate = false,

                theme = theme,
                translate = translate
            )
        }.let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.addAll(it)
            viewItemList.add(SpaceViewItem(id = "2", height = actionHeight.get()))
        }

        postDifferentValueIfActive(viewItemList)
    }

    val speakInfo: LiveData<SpeakInfo> = listenerSources(size, theme, translate, isSupportSpeak, speakState) {

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

        postDifferentValue(info)
    }

    val listenInfo: LiveData<ListenInfo> = listenerSources(size, theme, translate, isSupportListen, listenState) {

        val listenState = listenState.value

        val info = ListenInfo(

            image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
                R.drawable.ic_volume_24dp
            } else {
                R.drawable.ic_pause_24dp
            },

            isShow = isSupportListen.value == true,
            isLoading = listenState.isStart()
        )

        postDifferentValue(info)
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

    val resultInfo: LiveData<ResultInfo> = listenerSources(size, theme, translate, isCorrect, speakState) {

        val theme = theme.value ?: return@listenerSources

        val speakState = speakState.value

        val speakResult = speakState?.toSuccess()?.data.orEmpty()

        val isCorrect = isCorrect.value == true

        val background = Background(
            strokeColor = if (isCorrect) theme.colorPrimary else theme.colorError,
            strokeWidth = DP.DP_1,
            cornerRadius = DP.DP_8,
            backgroundColor = if (isCorrect) theme.colorPrimaryVariant else theme.colorErrorVariant
        )

        val info = ResultInfo(
            result = speakResult
                .with(ForegroundColorSpan(if (isCorrect) theme.colorOnPrimaryVariant else theme.colorOnErrorVariant)),
            isShow = speakState.isSuccess(),
            background = background
        )

        postDifferentValue(info)
    }


    fun updateText(it: String) {

        text.postDifferentValue(it)
    }

    fun updateSupportSpeak(it: Boolean) {

        isSupportListen.postDifferentValue(it)
    }

    fun startListen(text: String? = null, voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartReadingUseCase.Param(
            text = text ?: this@SpeakViewModel.text.value.orEmpty(),

            languageCode = inputLanguage.value?.id ?: Language.EN,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

        listenState.postValue(ResultState.Start)

        var job: Job? = null
        job = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

            listenState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopListen() = viewModelScope.launch(handler + Dispatchers.IO) {

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
        val result: CharSequence,

        val isShow: Boolean,

        val background: Background
    )

    data class SpeakInfo(
        val anim: Int?,
        val image: Int?,
        val isShow: Boolean,
        val isLoading: Boolean
    )

    data class ListenInfo(
        val image: Int,
        val isShow: Boolean,
        val isLoading: Boolean
    )
}