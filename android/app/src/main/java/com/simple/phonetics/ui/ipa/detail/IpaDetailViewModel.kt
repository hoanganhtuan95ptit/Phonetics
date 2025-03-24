package com.simple.phonetics.ui.ipa.detail

import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IpaDetailViewModel(
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase,
    private val checkSupportSpeakAsyncUseCase: CheckSupportSpeakAsyncUseCase
) : BaseViewModel() {

    private var job: Job? = null

    val ipa: LiveData<Ipa> = MediatorLiveData()

    val title: LiveData<String> = combineSources(ipa, translate) {

        val ipa = ipa.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        postDifferentValueIfActive(translate["ipa_detail_screen_" + ipa.type.lowercase()])
    }


    val listenState: LiveData<ResultState<String>> = MediatorLiveData()


    @VisibleForTesting
    val speakState: LiveData<ResultState<Boolean>> = mediatorLiveData {

        checkSupportSpeakAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val isSupportSpeak: LiveData<Boolean> = combineSources(speakState) {

        postDifferentValue(speakState.value?.toSuccess()?.data == true)
    }


    val isSupportListen: LiveData<Boolean> = MediatorLiveData(true)


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(ipa, inputLanguage, outputLanguage, phoneticCodeSelected) {

        postValue(ResultState.Start)

        val param = GetPhoneticsAsyncUseCase.Param(
            textNew = ipa.get().examples.joinToString(separator = " ") { it },

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

    @VisibleForTesting
    val phoneticsViewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, ipa, phoneticCodeSelected, phoneticsState, isSupportSpeak, isSupportListen, listenState) {

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val ipa = ipa.value ?: return@listenerSources
        val state = phoneticsState.value ?: return@listenerSources
        val listenState = listenState.value
        val phoneticsCode = phoneticCodeSelected.value ?: return@listenerSources

        state.doStart {

            postDifferentValue(getLoadingViewItem(theme))
            return@listenerSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        IpaDetailViewItem(
            id = ipa.ipa,

            data = ipa,

            ipa = ipa.ipa.with(ForegroundColorSpan(theme.colorOnSurface)),

            image = if (listenState.isRunning()) {
                R.drawable.ic_pause_black_24dp
            } else {
                R.drawable.ic_play_black_24dp
            },
            isShowLoading = listenState.isStart(),

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = ipa.BackgroundColor(theme)
            )
        ).let {

            viewItemList.add(it)
        }

        val listItem = state.toSuccess()?.data.orEmpty()

        TitleViewItem(
            id = "TITLE_EXAMPLE",
            text = translate["ipa_detail_screen_title_example"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
        ).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_EXAMPLE_AND_IPA_0", height = DP.DP_40))
            viewItemList.add(it)
        }

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,

                isShowSpeak = false,

                isSupportSpeak = isSupportSpeak.value == true,
                isSupportListen = isSupportListen.value == true,
                isSupportTranslate = false,

                theme = theme,
                translate = translate
            )
        }.let {

            viewItemList.addAll(it)
        }

        postDifferentValueIfActive(viewItemList)
    }

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(phoneticsViewItemList) {

        val list = arrayListOf<ViewItem>()

        phoneticsViewItemList.value.orEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_PHONETICS", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        postDifferentValueIfActive(list)
    }

    fun updateIpa(ipa: Ipa) {

        this.ipa.postDifferentValue(ipa)
    }

    fun updateSupportListen(it: Boolean) {

        isSupportListen.postDifferentValue(it)
    }

    fun startListen(data: Ipa) {

        job?.cancel()

        job = viewModelScope.launch(handler + Dispatchers.IO) {

            startListenWait(data)
        }
    }

    fun startListen(text: String, voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        listenState.postValue(ResultState.Start)

        val param = StartReadingUseCase.Param(
            text = text,

            languageCode = inputLanguage.value?.id ?: Language.EN,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

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

    suspend fun startListenWait(data: Ipa) = channelFlow {

        listenState.postValue(ResultState.Start)

        val mediaPlayer = MediaPlayer().apply {

            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(data.voice)
            prepareAsync()

            setOnPreparedListener {
                listenState.postValue(ResultState.Running(""))
                start() // Bắt đầu phát nhạc khi đã chuẩn bị xong
            }

            setOnErrorListener { mp, what, extra ->

                listenState.postValue(ResultState.Failed(RuntimeException("")))
                trySend(Unit)

                true // Trả về true nếu đã xử lý lỗi
            }

            setOnCompletionListener { mp ->

                listenState.postValue(ResultState.Success(""))
                trySend(Unit)
            }
        }

        awaitClose {

            mediaPlayer.reset()
            mediaPlayer.release()
        }
    }.first()

    private fun getLoadingViewItem(theme: AppTheme): List<ViewItem> = arrayListOf<ViewItem>().apply {

        val background = Background(
            cornerRadius = DP.DP_24,
            backgroundColor = theme.colorLoading
        )

        add(IpaDetailLoadingViewItem(id = "1", background = background))

        add(SpaceViewItem(id = "2", height = DP.DP_24))

        addAll(getPhoneticLoadingViewItem(theme = theme))
    }
}