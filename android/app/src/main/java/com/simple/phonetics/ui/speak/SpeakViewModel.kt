package com.simple.phonetics.ui.speak

import android.content.res.Resources
import android.util.TypedValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.image.ImageRes
import com.simple.image.RichImage
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
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toPronunciationColor
import com.simple.phonetics.utils.spans.TextSize
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
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
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


    val title: StateFlow<RichText> = combineState(
        themes,
        strings,
        initialValue = emptyText()
    ) { themes, strings ->
        value = strings.getOrKey("speak_screen_title").with(ForegroundColor(themes.colorOnSurface))
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
        sentenceScore,
        initialValue = emptyList()
    ) { size, themes, strings, state, sentenceScore ->

        if (state.isStart()) {

            value = getPhoneticLoadingViewItem(theme = themes)
            return@combineState
        }


        val viewItemList = arrayListOf<ViewItem>()

        fun Int?.getColor() = this?.toPronunciationColor() ?: themes.colorPrimary

        val wordScoreMap = sentenceScore?.wordScores
            .orEmpty()
            .associateBy { it.word.lowercase() }
            .toMutableMap()


        state.toSuccess()?.data.orEmpty().flatMap {

            it.phonetics
        }.mapIndexed { i, it ->

            val ipaList = it.ipaValueList
            val ipa = ipaList.joinToString(separator = " - ")

            val text = it.text

            val wordScore = wordScoreMap.remove(text.lowercase())
            val score = wordScore?.score

            // Lấy grapheme + điểm trực tiếp từ PhonemeScore.grapheme (được điền bởi pipeline).
            // Group theo grapheme, lấy min score để màu phản ánh âm tệ nhất trong cụm chữ.
            val graphemeScores: List<Pair<String, Int>> = wordScore?.phonemeScores
                ?.filter { it.errorType != ErrorType.INSERTION && it.grapheme != null }
                ?.groupBy { it.grapheme!! }
                ?.map { (g, list) -> g to list.minOf { it.score } }
                .orEmpty()

            // Build rich text: màu mặc định + size cho text/ipa, rồi override màu
            // theo điểm cho từng cụm chữ (apply CUỐI cùng để đè).
            var textDisplay = "$text\n$ipa"
                .with(ForegroundColor(themes.colorOnSurface))
                .with(text, Bold, TextSize(16))
                .with(ipa, TextSize(12), ForegroundColor(if (ipaList.size > 1) themes.colorPrimary else themes.colorError))
            for ((chunk, chunkScore) in graphemeScores) {
                textDisplay = textDisplay.with(chunk, ForegroundColor(chunkScore.getColor()))
            }

            PhoneticsViewItem2(
                id = "$text $i",
                text = text,
                textDisplay = textDisplay,
                hasStroke = score != null,
                strokeColor = score.getColor()
            )
        }.let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.addAll(it)
        }

        value = viewItemList
    }

    val viewItemMap: MutableStateFlow<ConcurrentHashMap<Double, List<ViewItem>>> = MutableStateFlow(ConcurrentHashMap())

    val viewItemList: StateFlow<List<ViewItem>> = combineState(
        actionHeightFlow,
        viewItemMap,
        initialValue = emptyList()
    ) { actionHeight, map ->

        val viewItemList = arrayListOf<ViewItem>()

        map.toList().sortedBy {
            it.first
        }.flatMap {
            it.second
        }.let {
            viewItemList.addAll(it)
        }

        viewItemList.add(SpaceViewItem(id = "2", height = actionHeight))

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
                ImageRes(data = R.drawable.ic_microphone_24dp, colorFilter = themes.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_microphone_slash_24dp, colorFilter = themes.colorPrimary)
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
        initialValue = ReadingInfo(ImageRes(0), false, false)
    ) { themes, strings, isSupportReading, readingState ->

        value = ReadingInfo(

            image = if (readingState.isIdle() || readingState.isStart() || readingState.isCompleted()) {
                ImageRes(data = R.drawable.ic_volume_24dp, colorFilter = themes.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_pause_24dp, colorFilter = themes.colorPrimary)
            },

            isShow = isSupportReading,
            isLoading = readingState.isStart()
        )
    }

    val copyInfo: StateFlow<CopyInfo> = combineState(
        themes,
        strings,
        initialValue = CopyInfo(ImageRes(0), false, emptyText())
    ) { themes, strings ->

        value = CopyInfo(

            image = ImageRes(R.drawable.ic_copy_24dp, themes.colorPrimary),

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

    val isCorrectEvent: LiveData<Event<Boolean>> = isCorrect.mapNotNull { it }
        .asLiveData()
        .toEvent() as LiveData<Event<Boolean>>


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
                .with(ForegroundColor(if (isCorrect) themes.colorOnPrimaryVariant else themes.colorOnErrorVariant)),
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

    fun startReading(text: String? = null) = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartReadingUseCase.Param(
            text = text ?: this@SpeakViewModel.text.value
        )

        readingState.value = ResultState.Start

        var job: Job? = null
        job = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

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

    fun Float.spToPx(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this,
            Resources.getSystem().displayMetrics
        )
    }

    fun Int.spToPx(): Int {
        return this.toFloat().spToPx().toInt()
    }

    data class ResultInfo(
        val result: RichText,

        val isShow: Boolean,

        val background: Background
    )

    data class CopyInfo(
        val image: RichImage,

        val isShow: Boolean,
        val messageThankUser: RichText,
    )

    data class SpeakInfo(
        val anim: Int?,
        val image: RichImage?,
        val isShow: Boolean,
        val isLoading: Boolean
    )

    data class ReadingInfo(
        val image: RichImage,
        val isShow: Boolean,
        val isLoading: Boolean
    )
}