package com.simple.phonetics.ui.speak

import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.toJson
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.toRich
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
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
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
import com.simple.phonetics.ui.common.adapters.PhoneticsViewItem2
import com.simple.phonetics.ui.speak.adapters.ScoreResultViewItem
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case.ErrorType
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case.SentenceScore
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.spans.TextSize
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
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnSurface
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

    val sentenceScore: LiveData<SentenceScore> = MediatorLiveData<SentenceScore>()


    val speakState: LiveData<ResultState<String>> = MediatorLiveData()

    val readingState: LiveData<ResultState<String>> = MediatorLiveData()


    val phoneticsState: LiveData<ResultState<List<Sentence>>> = combineSources(text, inputLanguage, outputLanguage, phoneticCodeSelected) {

        val param = GetPhoneticsAsyncUseCase.Param(
            textNew = text.get(),

            isReverse = false,
            isSaveToHistory = false,
            phoneticCode = phoneticCodeSelected.get(),
            inputLanguageCode = inputLanguage.get().id,
            outputLanguageCode = outputLanguage.get().id
        )

        getPhoneticsAsyncUseCase.execute(param).collect {

            Log.d("tuanha", "${it.toJson()}: ")
            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(size, theme, translate, actionHeight, phoneticCodeSelected, phoneticsState, isSupportReading, sentenceScore) {

        val size = size.value ?: return@listenerSourcesWithDiff
        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val state = phoneticsState.value ?: return@listenerSourcesWithDiff
        val sentenceScore = sentenceScore.value

        state.doStart {

            postValue(getPhoneticLoadingViewItem(theme = theme))
            return@listenerSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        fun Int?.getColor() = if (this == null) {
            theme.colorPrimary
        } else if (this in 0..50) {
            Color.RED
        } else if (this in 50..80) {
            Color.YELLOW
        } else {
            Color.GREEN
        }

        val wordScoreMap = sentenceScore?.wordScores
            .orEmpty()
            .associateBy { it.word.lowercase() }
            .toMutableMap()

        sentenceScore?.let { score ->
            val errorCount = score.errors.size
            val subtitle = when {
                score.finalScore >= 90 -> "Xuất sắc!"
                score.finalScore >= 70 -> "Khá tốt — còn $errorCount âm cần luyện"
                score.finalScore >= 50 -> "Cần cố gắng thêm — còn $errorCount âm cần luyện"
                else                   -> "Hãy luyện tập thêm — còn $errorCount âm cần luyện"
            }
            viewItemList.add(SpaceViewItem(id = "score_space_top", height = DP.DP_16))
            viewItemList.add(
                ScoreResultViewItem(
                    id = "score_result",
                    data = score,
                    subtitle = subtitle
                )
            )
        }

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
                .with(ForegroundColor(theme.colorOnSurface))
                .with(text, TextSize(16))
                .with(ipa, TextSize(12), ForegroundColor(if (ipaList.size > 1) theme.colorPrimary else theme.colorError))
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

        sentenceScore?.errors?.map {
            val msg = when (it.errorType) {
                ErrorType.SUBSTITUTION -> "• /${it.phoneme}/ đọc thành /${it.substitutedWith}/  (\"${it.wordContext}\")"
                ErrorType.DELETION     -> "• /${it.phoneme}/ bị nuốt  (\"${it.wordContext}\")"
                ErrorType.INSERTION    -> "• /${it.phoneme}/ thêm thừa"
                else                   -> ""
            }

            NoneTextViewItem(
                id = it.phoneme,
                text = msg.toRich(),
                size = Size(width = size.width)
            )
        }?.let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.addAll(it)
        }

        viewItemList.add(SpaceViewItem(id = "2", height = actionHeight.get()))

        postValueIfActive(viewItemList)
    }

    val speakInfo: LiveData<SpeakInfo> = listenerSourcesWithDiff(size, theme, translate, isSupportSpeak, speakState) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

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
                ImageRes(data = R.drawable.ic_microphone_24dp, colorFilter = theme.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_microphone_slash_24dp, colorFilter = theme.colorPrimary)
            },

            isLoading = speakState.isStart(),

            isShow = isSupportSpeak.value == true,
        )

        postValue(info)
    }

    val readingInfo: LiveData<ReadingInfo> = listenerSourcesWithDiff(size, theme, translate, isSupportReading, readingState) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

        val listenState = readingState.value

        val info = ReadingInfo(

            image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
                ImageRes(data = R.drawable.ic_volume_24dp, colorFilter = theme.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_pause_24dp, colorFilter = theme.colorPrimary)
            },

            isShow = isSupportReading.value == true,
            isLoading = listenState.isStart()
        )

        postValue(info)
    }

    val copyInfo: LiveData<CopyInfo> = listenerSourcesWithDiff(size, theme, translate) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

        val info = CopyInfo(

            image = ImageRes(R.drawable.ic_copy_24dp, theme.colorPrimary),

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