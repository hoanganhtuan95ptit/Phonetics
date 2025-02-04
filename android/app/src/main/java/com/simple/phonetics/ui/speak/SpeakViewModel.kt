package com.simple.phonetics.ui.speak

import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.LoadingViewItem
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.R
import com.simple.phonetics.SpeakState
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.voice.StartListenUseCase
import com.simple.phonetics.domain.usecase.voice.StopListenUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.CommonViewModel
import com.simple.phonetics.ui.speak.adapters.ImageStateViewItem
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.isSuccess
import com.simple.state.toFailed
import com.simple.state.toRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SpeakViewModel(
    private val stopListenUseCase: StopListenUseCase,
    private val startListenUseCase: StartListenUseCase,

    private val stopSpeakUseCase: StopSpeakUseCase,
    private val startSpeakUseCase: StartSpeakUseCase,

    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : CommonViewModel() {

    private val itemLoading = listOf(
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading)
    )

    val text: LiveData<String> = MediatorLiveData()

    @VisibleForTesting
    val phoneticsCode: LiveData<String> = MediatorLiveData()

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(text, inputLanguage, outputLanguage) {

        val param = GetPhoneticsAsyncUseCase.Param(
            text = text.get(),
            isReverse = false,
            saveToHistory = false,
            inputLanguageCode = inputLanguage.get().id,
            outputLanguageCode = outputLanguage.get().id
        )

        getPhoneticsAsyncUseCase.execute(param).collect {

            postValue(it)
        }
    }

    val phoneticsViewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, phoneticsCode, phoneticsState) {

        val theme = theme.get()
        val translate = translate.get()

        val state = phoneticsState.get()
        val phoneticsCode = phoneticsCode.get()
//        val isSupportTranslate = isSupportTranslate.get()

        state.doStart {

            postDifferentValue(itemLoading)
            return@combineSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        val listItem = state.toSuccess()?.data.orEmpty()
        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,
                isSupportTranslate = false,

                theme = theme,
                translate = translate
            )
        }.let {

            viewItemList.addAll(it)
        }

        postDifferentValueIfActive(viewItemList)
    }

    val listenState: LiveData<ResultState<String>> = MediatorLiveData()

    @VisibleForTesting
    val isSupportListen: LiveData<Boolean> = MediatorLiveData(true)


    val speakState: LiveData<ResultState<String>> = MediatorLiveData()

    @VisibleForTesting
    val isSupportSpeak: LiveData<Boolean> = MediatorLiveData(true)


    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, listenState, isSupportListen, speakState, isSupportSpeak, phoneticsViewItemList) {

        val list = arrayListOf<ViewItem>()

        phoneticsViewItemList.value.orEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_PHONETICS", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        Log.d("tuanha", "viewItemList: ")
        val size = size.value ?: return@listenerSources

        Log.d("tuanha", "viewItemList:1 ")
        val speakState = speakState.value
        val isSupportSpeak = isSupportSpeak.value ?: false

        val listenState = listenState.value
        val isSupportListen = isSupportListen.value ?: false

        if (speakState is ResultState.Success) NoneTextViewItem(
            id = "1",

            text = speakState.data,

            textStyle = TextStyle(
                textGravity = Gravity.CENTER
            ),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_PHONETICS_1", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_56))
        }

        if (isSupportListen) ImageStateViewItem(
            id = ID.LISTEN,

            image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
                R.drawable.ic_play_24dp
            } else {
                R.drawable.ic_pause_24dp
            },

            isLoading = listenState.isStart(),

            size = Size(
                width = (size.width - 2 * DP.DP_24) / 3,
                height = DP.DP_56
            ),

            padding = Padding(
                left = DP.DP_16,
                top = DP.DP_16,
                right = DP.DP_16,
                bottom = DP.DP_16
            )
        ).let {

            list.add(it)
        } else NoneTextViewItem(
            id = "ID_SPEAK_1",
            text = "",

            size = Size(
                width = (size.width - 2 * DP.DP_24) / 3,
                height = DP.DP_56
            ),
        ).let {

            list.add(it)
        }


        if (isSupportSpeak) ImageStateViewItem(
            id = ID.SPEAK,

            image = if (speakState.isRunning()) {
                R.drawable.ic_speaking_24dp
            } else if (speakState == null || speakState.isStart() || speakState.isCompleted()) {
                R.drawable.ic_microphone_24dp
            } else {
                R.drawable.ic_microphone_slash_24dp
            },

            isLoading = speakState.isStart(),

            size = Size(
                width = (size.width - 2 * DP.DP_24) / 3,
                height = DP.DP_70
            ),

            imageSize = Size(
                width = DP.DP_56,
                height = DP.DP_56
            )
        ).let {

            list.add(it)
        }

        NoneTextViewItem(
            id = "ID_SPEAK_2",
            text = "",

            size = Size(
                width = (size.width - 2 * DP.DP_24) / 3,
                height = DP.DP_56
            ),
        ).let {

            list.add(it)
        }

        Log.d("tuanha", "viewItemList3: ")
        postDifferentValueIfActive(list)
    }

    fun updateText(it: String) {

        text.postDifferentValue(it)
    }

    fun updateSupportSpeak(it: Boolean) {

        isSupportListen.postDifferentValue(it)
    }

    fun updatePhoneticSelect(it: String) {

        phoneticsCode.postDifferentValue(it)
    }


    fun startListen(voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        listenState.postValue(ResultState.Start)

        val param = StartListenUseCase.Param(
            text = text.value.orEmpty(),

            languageCode = inputLanguage.value?.id ?: Language.EN,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

        var job: Job? = null

        job = startListenUseCase.execute(param).launchCollect(viewModelScope) { state ->

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

        stopListenUseCase.execute()
    }


    fun startSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        speakState.postValue(ResultState.Start)

        val param = StartSpeakUseCase.Param(
            languageCode = inputLanguage.value?.id ?: Language.EN,
        )

        var job: Job? = null

        job = startSpeakUseCase.execute(param).launchCollect(viewModelScope) { state ->

            Log.d("tuanha", "startSpeak: ${state.javaClass.simpleName}")

            speakState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopSpeakUseCase.execute()
    }

    object ID {

        const val SPEAK = "SPEAK"
        const val LISTEN = "LISTEN"
    }
}