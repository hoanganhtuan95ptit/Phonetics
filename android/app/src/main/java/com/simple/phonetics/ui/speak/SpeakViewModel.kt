package com.simple.phonetics.ui.speak

import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.coreapp.utils.extentions.toPx
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.R
import com.simple.phonetics.SpeakState
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.domain.usecase.speak.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.speak.StopSpeakUseCase
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
import com.simple.phonetics.ui.common.adapters.PhoneticsViewItem2
import com.simple.phonetics.ui.common.adapters.SpaceViewItem2
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toPronunciationColor
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isIdle
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.isSuccess
import com.simple.state.mapToData
import com.simple.state.toRunning
import com.simple.state.toSuccess
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.ColorFilter
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.emptyText
import com.simple.ui.precompute.text.span.Bold
import com.simple.ui.precompute.text.span.ForegroundColor
import com.simple.ui.precompute.text.span.TextSize
import com.simple.ui.precompute.text.with
import com.simple.ui.precompute.text.withFirst
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class SpeakViewModel(
    private val stopSpeakUseCase: StopSpeakUseCase,
    private val startSpeakUseCase: StartSpeakUseCase,

    private val stopReadingUseCase: StopReadingUseCase,
    private val startReadingUseCase: StartReadingUseCase,

    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase
) : BaseActionViewModel() {


    val jobQueue = JobQueue()


    val text: MutableStateFlow<String> = MutableStateFlow("")

    val sentenceScore: MutableStateFlow<SentenceScore?> = MutableStateFlow(null)


    val speakState: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.Idle)

    val readingState: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.Idle)


    val title: StateFlow<BigText> = combineState(
        themes,
        strings,
        initialValue = emptyText()
    ) { themes, strings ->
        value = strings.getOrKey("speak_screen_title")
            .with(ForegroundColor(themes.colorOnSurface))
            .build()
    }

    val phoneticsState: StateFlow<ResultState<List<Sentence>>> = combineState(
        text,
        inputLanguageFlow,
        outputLanguageFlow,
        phoneticCodeSelectedFlow,
        ResultState.Idle as ResultState<List<Sentence>>
    ) { text, inputLanguage, outputLanguage, phoneticCodeSelected ->

        val param = GetPhoneticsAsyncUseCase.Param(
            textNew = text,

            isReverse = false,
            isSaveToHistory = false,
            phoneticCode = phoneticCodeSelected,
            inputLanguageCode = inputLanguage?.id ?: Language.EN,
            outputLanguageCode = outputLanguage.id
        )

        fun Sentence.copy() = Sentence(this.text).apply {
            phonetics = this@copy.phonetics
            translateState = this@copy.translateState
        }

        getPhoneticsAsyncUseCase.execute(param).mapToData { sentences ->

            sentences.toMutableList().map { it.copy() }
        }.collect {

            value = it
        }
    }

    val phoneticViewItemList: StateFlow<List<ViewItem>> = combineState(
        sizes,
        themes,
        strings,
        phoneticsState,
        isSupportReadingFlow,
        sentenceScore,
        initialValue = emptyList()
    ) { sizes, themes, strings, phoneticState, isSupportReading, sentenceScore ->

        if (phoneticState.isStart()) {

            value = getPhoneticLoadingViewItem(theme = themes)
            return@combineState
        }

        fun Int?.getColor() = this?.toPronunciationColor() ?: themes.colorPrimary


        val viewItemList = arrayListOf<ViewItem>()


        val wordScoreMap = sentenceScore?.wordScores
            .orEmpty()
            .associateBy { it.word.lowercase() }
            .toMutableMap()

        val iconDisplay = R.drawable.ic_volume_black_24dp.toBuilder()
            .addTransform(ColorFilter(themes.colorOnSurface))
            .build()

        phoneticState.toSuccess()?.data.orEmpty().flatMap {

            it.phonetics
        }.mapIndexed { i, it ->

            val id = "${it.text} $i"


            val ipaList = it.ipaValueList
            val ipa = ipaList.joinToString(separator = " - ")

            val text = it.text

            val wordScore = wordScoreMap.remove(text.lowercase())
            val score = wordScore?.score

            var textDisplay = text
                .with(ForegroundColor(themes.colorOnSurface))
                .withFirst(text, Bold, TextSize(16.toPx()))

            wordScore?.phonemeScores?.filter {
                it.errorType != ErrorType.INSERTION && it.grapheme != null
            }?.groupBy {
                it.grapheme!!
            }?.map { (g, list) ->
                g to list.minOf { it.score }
            }.orEmpty().forEach { (chunk, chunkScore) ->

                textDisplay = textDisplay.withFirst(chunk, ForegroundColor(chunkScore.getColor()))
            }

            val phoneticDisplay = ipa
                .with(TextSize(12.toPx()), ForegroundColor(if (ipaList.size > 1) themes.colorPrimary else themes.colorError))


            PhoneticsViewItem2(
                id = id,
                text = text,

                textDisplay = textDisplay.build(),
                phoneticDisplay = phoneticDisplay.build(),

                iconShow = isSupportReading,
                iconDisplay = iconDisplay,

                onlyReading = isSupportReading,

                strokeShow = score != null,
                strokeColor = score.getColor(),

                maxWidth = sizes.width
            )
        }.let {

            viewItemList.add(SpaceViewItem2(id = "1", maxWidth = sizes.width, height = 16.dp()))
            viewItemList.addAll(it)
        }

        value = viewItemList
    }

    val viewItemMap: MutableStateFlow<ConcurrentHashMap<Double, List<ViewItem>>> = MutableStateFlow(ConcurrentHashMap())

    val viewItemList: StateFlow<List<ViewItem>> = combineState(
        sizes,
        actionHeightFlow,
        viewItemMap,
        initialValue = emptyList()
    ) { sizes, actionHeight, map ->

        val viewItemList = arrayListOf<ViewItem>()

        if (map.isEmpty()) {
            return@combineState
        }

        map.toList().sortedBy {
            it.first
        }.flatMap {
            it.second
        }.let {
            viewItemList.addAll(it)
        }

        viewItemList.add(SpaceViewItem2(id = "2", maxWidth = sizes.width, height = actionHeight.toFloat()))

        value = viewItemList
    }

    val speakInfo: StateFlow<SpeakInfo> = combineState(
        themes,
        strings,
        isSupportSpeakFlow,
        speakState,
        initialValue = SpeakInfo(null, null, false, false)
    ) { themes, strings, isSupportSpeak, speakState ->

        value = SpeakInfo(

            anim = if (speakState.isRunning()) {
                R.raw.anim_recording
            } else {
                null
            },

            image = if (speakState.isRunning()) {
                null
            } else if (speakState.isIdle() || speakState.isStart() || speakState.isCompleted()) {
                R.drawable.ic_microphone_black_24dp.toBuilder()
                    .addTransform(ColorFilter(themes.colorPrimary))
                    .build()
            } else {
                R.drawable.ic_microphone_slash_black_24dp.toBuilder()
                    .addTransform(ColorFilter(themes.colorPrimary))
                    .build()
            },

            isLoading = speakState.isStart(),

            isShow = isSupportSpeak,
        )
    }

    val readingInfo: StateFlow<ReadingInfo> = combineState(
        themes,
        strings,
        isSupportReadingFlow,
        readingState,
        initialValue = ReadingInfo(emptyImage(), false, false)
    ) { themes, strings, isSupportReading, readingState ->

        value = ReadingInfo(

            image = if (readingState.isIdle() || readingState.isStart() || readingState.isCompleted()) {
                R.drawable.ic_volume_black_24dp.toBuilder()
                    .addTransform(ColorFilter(themes.colorPrimary))
                    .build()
            } else {
                R.drawable.ic_pause_black_24dp.toBuilder()
                    .addTransform(ColorFilter(themes.colorPrimary))
                    .build()
            },

            isShow = isSupportReading,
            isLoading = readingState.isStart()
        )
    }

    val copyInfo: StateFlow<CopyInfo> = combineState(
        themes,
        strings,
        initialValue = CopyInfo(emptyImage(), false, emptyText())
    ) { themes, strings ->

        value = CopyInfo(

            image = R.drawable.ic_copy_black_24dp.toBuilder()
                .addTransform(ColorFilter(themes.colorPrimary))
                .build(),

            isShow = true,
            messageThankUser = emptyText()
        )
    }

    val isCorrect: StateFlow<Boolean?> = combineState(
        text,
        speakState,
        initialValue = null
    ) { text, speakState ->

        if (!speakState.isSuccess()) {

            value = null
            return@combineState
        }

        val speakResult = speakState.toSuccess()?.data.orEmpty()

        value = speakResult.equals(text, true)
    }

    val isCorrectEvent: Flow<Event<Boolean>> = isCorrect.mapNotNull {
        it
    }.map {
        it.toEvent()
    }


    val resultInfo: StateFlow<ResultInfo> = combineState(
        themes,
        strings,
        text,
        speakState,
        initialValue = ResultInfo(emptyText(), false, Background())
    ) { themes, strings, text, speakState ->

        val speakResult = speakState.toRunning()?.data
            ?: speakState.toSuccess()?.data.orEmpty()

        if (speakResult in SpeakState.stateList) {

            value = ResultInfo(emptyText(), false, Background())
            return@combineState
        }


        val isCorrect = speakResult.equals(text, true)

        val background = Background(
            strokeColor = if (isCorrect) themes.colorPrimary else themes.colorError,
            strokeWidth = DP.DP_1,
            cornerRadius = DP.DP_8,
            backgroundColor = if (isCorrect) themes.colorPrimaryVariant else themes.colorErrorVariant
        )

        value = ResultInfo(
            result = speakResult
                .with(ForegroundColor(if (isCorrect) themes.colorOnPrimaryVariant else themes.colorOnErrorVariant))
                .build(),
            isShow = speakResult.isNotBlank(),
            background = background
        )
    }

    init {

        phoneticViewItemList.onEach {

            add(1, it)
        }.launchIn(viewModelScope)
    }

    fun add(order: Int, list: List<ViewItem>) {
        add(order.toDouble(), list)
    }

    fun add(order: Double, list: List<ViewItem>) = jobQueue.submit {
        val map = ConcurrentHashMap(viewItemMap.value)
        map[order] = list
        viewItemMap.value = map
    }

    fun updateText(it: String) {

        text.value = it
    }

    fun startReading(text: String? = null) = launchWithTag("reading") {

        val param = StartReadingUseCase.Param(
            text = text ?: this@SpeakViewModel.text.value
        )

        readingState.value = ResultState.Start

        var job: Job? = null
        job = startReadingUseCase.execute(param).launchCollect(this) { state ->

            readingState.value = state

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
            languageCode = inputLanguageFlow.first()?.id ?: Language.EN,
        )

        speakState.value = ResultState.Start

        startSpeakUseCase.execute(param).launchCollect(viewModelScope) { state ->

            speakState.value = state
        }
    }

    fun stopSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopSpeakUseCase.execute()
    }

    data class ResultInfo(
        val result: BigText,

        val isShow: Boolean,

        val background: Background
    )

    data class CopyInfo(
        val image: BigImage,

        val isShow: Boolean,
        val messageThankUser: BigText,
    )

    data class SpeakInfo(
        val anim: Int?,
        val image: BigImage?,
        val isShow: Boolean,
        val isLoading: Boolean
    )

    data class ReadingInfo(
        val image: BigImage,
        val isShow: Boolean,
        val isLoading: Boolean
    )
}