package com.simple.phonetics.ui.ipa.detail

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.dao.entities.Ipa
import com.simple.phonetics.BRANCH
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.entities.Text
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailViewItem
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.phonetics.utils.spans.RoundedBackground
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
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : BaseViewModel() {

    private var job: Job? = null

    val ipa: LiveData<Ipa> = MediatorLiveData()

    val title: LiveData<String> = combineSources(ipa, translate) {

        val ipa = ipa.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        postDifferentValueIfActive(translate["ipa_detail_screen_" + ipa.type.lowercase()])
    }


    val readingState: LiveData<ResultState<String>> = MediatorLiveData()

    @VisibleForTesting
    val ipaViewItemList: LiveData<List<ViewItem>> = listenerSources(theme, ipa, readingState) {

        val theme = theme.value ?: return@listenerSources

        val ipa = ipa.value ?: return@listenerSources
        val readingState = readingState.value

        val viewItemList = arrayListOf<ViewItem>()

        IpaDetailViewItem(
            id = ipa.ipa,

            data = ipa,

            ipa = ipa.ipa.with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),

            image = if (readingState.isRunning()) {
                R.drawable.ic_pause_black_24dp
            } else {
                R.drawable.ic_play_black_24dp
            },
            isShowLoading = readingState.isStart(),

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = ipa.BackgroundColor(theme)
            )
        ).let {

            viewItemList.add(it)
        }

        postValue(viewItemList)
    }


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
    val phoneticsViewItemList: LiveData<List<ViewItem>> = listenerSources(theme, translate, phoneticsState, phoneticCodeSelected, isSupportSpeak, isSupportReading) {

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val state = phoneticsState.value ?: return@listenerSources
        val phoneticsCode = phoneticCodeSelected.value ?: return@listenerSources

        state.doStart {

            postValue(getLoadingViewItem(theme))
            return@listenerSources
        }

        val viewItemList = arrayListOf<ViewItem>()


        TitleViewItem(
            id = "TITLE_EXAMPLE",
            text = translate["ipa_detail_screen_title_example"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        ).let {

            viewItemList.add(it)
        }

        val listItem = state.toSuccess()?.data.orEmpty()

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,

                isShowSpeak = false,

                isSupportSpeak = isSupportSpeak.value == true,
                isSupportListen = isSupportReading.value == true,
                isSupportTranslate = false,

                theme = theme,
                translate = translate
            )
        }.let {

            viewItemList.addAll(it)
        }

        postDifferentValueIfActive(viewItemList)
    }

    val gameResource: LiveData<Text> = combineSources(ipa) {

        val ipa = ipa.get()

        postValue(Text(ipa.ipa, Text.Type.IPA))
    }

    @VisibleForTesting
    val gameViewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, ipa, phoneticCodeSelected) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val ipa = ipa.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        if (!translate.containsKey("ipa_detail_screen_practice_with_games")) {

            postValue(emptyList())
            return@combineSources
        }


        val gameResource = Text(ipa.ipa, Text.Type.IPA)

        val param = GetPhoneticsRandomUseCase.Param(
            text = gameResource,
            resource = Word.Resource.Popular,
            phoneticsCode = phoneticCodeSelected,

            limit = 4,
            textLengthMin = 2,
            textLengthMax = 20
        )

        val phoneticList = getPhoneticsRandomUseCase.execute(param = param)

        if (phoneticList.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }


        val list = arrayListOf<ViewItem>()

        ClickTextViewItem(
            id = Id.GAME,
            data = gameResource,
            text = translate["ipa_detail_screen_practice_with_games"]
                .orEmpty()
                .replace("\$ipa", gameResource.text)
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorPrimary")))
                .with(gameResource.text, Bold, RoundedBackground(backgroundColor = theme.getOrTransparent("colorErrorVariant"), textColor = theme.getOrTransparent("colorOnErrorVariant"))),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT
            ),
            textStyle = TextStyle(
                textGravity = Gravity.CENTER
            ),
            textPadding = Padding(
                left = DP.DP_16,
                right = DP.DP_16
            ),
            textBackground = Background(
                strokeColor = theme.getOrTransparent("colorPrimary"),
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_76
            ),
            margin = Margin(
                marginHorizontal = DP.DP_40
            ),
            background = DEFAULT_BACKGROUND,

            imageLeft = null,
            imageRight = null
        ).let {

            list.add(it)
        }

        if (list.isNotEmpty()) {
            logAnalytics("game_ipa_show")
        }

        postValue(list)
    }


    val viewItemList: LiveData<List<ViewItem>> = listenerSources(ipaViewItemList, gameViewItemList, phoneticsViewItemList) {

        val list = arrayListOf<ViewItem>()

        ipaViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_IPA", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_40))
        }

        phoneticsViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_PHONETICS", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        gameViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_GAME", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        postDifferentValueIfActive(list)
    }

    fun updateIpa(ipa: Ipa) {

        this.ipa.postValue(ipa)
    }

    fun startReading(data: Ipa) {

        job?.cancel()

        job = viewModelScope.launch(handler + Dispatchers.IO) {

            startReadingWait(data)
        }
    }

    fun startReading(text: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        readingState.postValue(ResultState.Start)

        val param = StartReadingUseCase.Param(
            text = text
        )

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

    private suspend fun startReadingWait(data: Ipa) = channelFlow {

        readingState.postValue(ResultState.Start)

        var voice: String? = null


        if (data.voices.isNotEmpty()) {

            voice = data.voices[phoneticCodeSelected.value.orEmpty()]
        }

        if (voice == null) {

            voice = data.voice
        }

        voice = voice.replace("\$branch", BRANCH)

        if (BuildConfig.DEBUG) {

            voice = voice.replace("heads/main/", "heads/$BRANCH/")
        }


        val job = launch {

            kotlinx.coroutines.delay(1 * 60 * 1000)

            readingState.postValue(ResultState.Failed(RuntimeException("")))
            trySend(Unit)
        }


        val mediaPlayer = MediaPlayer().apply {

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            setAudioAttributes(audioAttributes)
            setDataSource(voice)
            prepareAsync()

            setOnPreparedListener {

                job.cancel()

                readingState.postValue(ResultState.Running(""))
                start() // Bắt đầu phát nhạc khi đã chuẩn bị xong
            }

            setOnErrorListener { _, _, _ ->

                readingState.postValue(ResultState.Failed(RuntimeException("")))
                trySend(Unit)

                true // Trả về true nếu đã xử lý lỗi
            }

            setOnCompletionListener { _ ->

                readingState.postValue(ResultState.Success(""))
                trySend(Unit)
            }
        }

        awaitClose {

            mediaPlayer.reset()
            mediaPlayer.release()
        }
    }.first()

    private fun getLoadingViewItem(theme: Map<String, Int>): List<ViewItem> = arrayListOf<ViewItem>().apply {

        val background = Background(
            cornerRadius = DP.DP_24,
            backgroundColor = theme.getOrTransparent("colorLoading")
        )

        add(IpaDetailLoadingViewItem(id = "1", background = background))

        add(SpaceViewItem(id = "2", height = DP.DP_24))

        addAll(getPhoneticLoadingViewItem(theme = theme))
    }
}