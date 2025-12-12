package com.simple.phonetics.ui.speak.services.pronunciation_assessment

import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.orZero
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichRange
import com.simple.coreapp.utils.ext.RichSpan
import com.simple.coreapp.utils.ext.RichStyle
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.toRich
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.speak.SpeakViewModel.ResultInfo
import com.simple.phonetics.ui.speak.SpeakViewModel.SpeakInfo
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.adapters.ListViewItem
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.adapters.NBestPronunciationAssessmentViewItem
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.entities.AssessmentResult
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.utils.PronunciationAssessmentUtils
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.get
import com.simple.phonetics.utils.exts.getNotNull
import com.simple.phonetics.utils.exts.listenerSourcesWithDiff
import com.simple.phonetics.utils.exts.mutableSharedFlow
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.simple.phonetics.utils.exts.value
import com.simple.phonetics.utils.spans.RoundedBackground
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.toFailed
import com.simple.state.toRunning
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import com.unknown.theme.utils.exts.colorSurface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn

class PronunciationAssessmentViewModel : BaseViewModel() {


    private val phonemeMap = mapOf(
        "r" to PhonemeConfig("/r/", 1),
        "iy" to PhonemeConfig("/iː/", 2),
        "d" to PhonemeConfig("/d/", 1),

        "dh" to PhonemeConfig("/ð/", 2),
        "ih" to PhonemeConfig("/ɪ/", 1),
        "s" to PhonemeConfig("/s/", 1),

        "eh" to PhonemeConfig("/ɛ/", 1),
        "n" to PhonemeConfig("/n/", 1),
        "t" to PhonemeConfig("/t/", 1),
        "ax" to PhonemeConfig("/ə/", 2)
    )

    val text = mutableSharedFlow<String> {

//        emit("Read this sentence.")
    }

    val buttonInfo = combineSourcesWithDiff<ButtonAssessmentInfo>(themeFlow) {

        val theme = themeFlow.getNotNull()

        val info = ButtonAssessmentInfo(
            isShow = true,

            text = "Chấm điểm phát âm Beta"
                .with(ForegroundColor(theme.colorPrimary))
                .with("Beta", Bold, RoundedBackground(backgroundColor = theme.colorErrorVariant, textColor = theme.colorOnErrorVariant)),

            background = Background(
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            )
        )

        emit(info)
    }

    val assessmentState = mutableSharedFlow<ResultState<String>> {

//        val text =
//            "{\"Id\":\"9ca283b939414b8e92f218649536ade7\",\"RecognitionStatus\":\"Success\",\"Offset\":2300000,\"Duration\":16000000,\"Channel\":0,\"DisplayText\":\"Readthissentence.\",\"SNR\":18.502283,\"NBest\":[{\"Confidence\":0.9504961,\"Lexical\":\"readthissentence\",\"ITN\":\"readthissentence\",\"MaskedITN\":\"readthissentence\",\"Display\":\"Readthissentence.\",\"PronunciationAssessment\":{\"AccuracyScore\":78.0,\"FluencyScore\":96.0,\"CompletenessScore\":100.0,\"PronScore\":86.0},\"Words\":[{\"Word\":\"read\",\"Offset\":2300000,\"Duration\":5000000,\"PronunciationAssessment\":{\"AccuracyScore\":79.0,\"ErrorType\":\"None\"},\"Syllables\":[{\"Syllable\":\"riyd\",\"Grapheme\":\"read\",\"PronunciationAssessment\":{\"AccuracyScore\":77.0},\"Offset\":2300000,\"Duration\":5000000}],\"Phonemes\":[{\"Phoneme\":\"r\",\"PronunciationAssessment\":{\"AccuracyScore\":84.0},\"Offset\":2300000,\"Duration\":2200000},{\"Phoneme\":\"iy\",\"PronunciationAssessment\":{\"AccuracyScore\":92.0},\"Offset\":4600000,\"Duration\":900000},{\"Phoneme\":\"d\",\"PronunciationAssessment\":{\"AccuracyScore\":61.0},\"Offset\":5600000,\"Duration\":1700000}]},{\"Word\":\"this\",\"Offset\":7800000,\"Duration\":3900000,\"PronunciationAssessment\":{\"AccuracyScore\":91.0,\"ErrorType\":\"None\"},\"Syllables\":[{\"Syllable\":\"dhihs\",\"Grapheme\":\"this\",\"PronunciationAssessment\":{\"AccuracyScore\":68.0},\"Offset\":7800000,\"Duration\":3900000}],\"Phonemes\":[{\"Phoneme\":\"dh\",\"PronunciationAssessment\":{\"AccuracyScore\":56.0},\"Offset\":7800000,\"Duration\":1500000},{\"Phoneme\":\"ih\",\"PronunciationAssessment\":{\"AccuracyScore\":84.0},\"Offset\":9400000,\"Duration\":900000},{\"Phoneme\":\"s\",\"PronunciationAssessment\":{\"AccuracyScore\":69.0},\"Offset\":10400000,\"Duration\":1300000}]},{\"Word\":\"sentence\",\"Offset\":12000000,\"Duration\":6300000,\"PronunciationAssessment\":{\"AccuracyScore\":64.0,\"ErrorType\":\"None\"},\"Syllables\":[{\"Syllable\":\"sehn\",\"Grapheme\":\"sen\",\"PronunciationAssessment\":{\"AccuracyScore\":81.0},\"Offset\":12000000,\"Duration\":2900000},{\"Syllable\":\"taxns\",\"Grapheme\":\"tence\",\"PronunciationAssessment\":{\"AccuracyScore\":52.0},\"Offset\":15000000,\"Duration\":3300000}],\"Phonemes\":[{\"Phoneme\":\"s\",\"PronunciationAssessment\":{\"AccuracyScore\":83.0},\"Offset\":12000000,\"Duration\":1400000},{\"Phoneme\":\"eh\",\"PronunciationAssessment\":{\"AccuracyScore\":72.0},\"Offset\":13500000,\"Duration\":400000},{\"Phoneme\":\"n\",\"PronunciationAssessment\":{\"AccuracyScore\":83.0},\"Offset\":14000000,\"Duration\":900000},{\"Phoneme\":\"t\",\"PronunciationAssessment\":{\"AccuracyScore\":82.0},\"Offset\":15000000,\"Duration\":700000},{\"Phoneme\":\"ax\",\"PronunciationAssessment\":{\"AccuracyScore\":68.0},\"Offset\":15800000,\"Duration\":1400000},{\"Phoneme\":\"n\",\"PronunciationAssessment\":{\"AccuracyScore\":7.0},\"Offset\":17300000,\"Duration\":400000},{\"Phoneme\":\"s\",\"PronunciationAssessment\":{\"AccuracyScore\":9.0},\"Offset\":17800000,\"Duration\":500000}]}]}]}"
//
//        emit(ResultState.Success(text.toObject<AssessmentResult>()))
    }

    val speakInfo = listenerSourcesWithDiff(themeFlow, translateFlow, isSupportSpeakFlow, assessmentState) {

        val assessmentState = assessmentState.value

        val info = SpeakInfo(

            anim = if (assessmentState.isRunning()) {
                R.raw.anim_recording
            } else {
                null
            },

            image = if (assessmentState.isRunning()) {
                null
            } else if (assessmentState == null || assessmentState.isStart() || assessmentState.isCompleted()) {
                R.drawable.ic_microphone_24dp
            } else {
                R.drawable.ic_microphone_slash_24dp
            },

            isLoading = assessmentState.isStart(),

            isShow = isSupportSpeakFlow.value == true,
        )

        emit(info)
    }

    val resultInfo = listenerSourcesWithDiff(themeFlow, assessmentState) {

        val theme = themeFlow.value ?: return@listenerSourcesWithDiff

        val assessmentResult = assessmentState.value?.toRunning()?.data.orEmpty()


        val info = ResultInfo(
            result = assessmentResult
                .with(ForegroundColor(theme.colorOnPrimaryVariant)),
            isShow = assessmentResult.isNotBlank(),
            background = Background(
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_1,
                cornerRadius = DP.DP_8,
                backgroundColor = theme.colorPrimaryVariant
            )
        )

        emit(info)
    }


    @VisibleForTesting
    val assessment = combineSourcesWithDiff(assessmentState) {

        val assessmentState = assessmentState.getNotNull()

        Log.d("tuanha", "assessmentState: ${assessmentState.toSuccess()?.data}")
        emit(assessmentState.toSuccess()?.data?.toObject<AssessmentResult>())
    }


    @VisibleForTesting
    val ipaState = mutableSharedFlowWithDiff {

        GetIpaStateAsyncUseCase.install.execute(param = GetIpaStateAsyncUseCase.Param(sync = false)).collect {

            emit(it)
        }
    }

    @VisibleForTesting
    val ipaMap = combineSourcesWithDiff(ipaState) {

        val ipaState = ipaState.getNotNull()

        ipaState.toSuccess()?.data.orEmpty().associateBy { it.ipa }.let {

            emit(it)
        }
    }


    val nBestViewItem = combineSourcesWithDiff(assessment) {

        val assessment = assessment.getNotNull().nbest.firstOrNull()?.pronunciationAssessment

        if (assessment == null) {

            emit(emptyList())
            return@combineSourcesWithDiff
        }

        val list = arrayListOf<ViewItem>()

        NBestPronunciationAssessmentViewItem(
            id = "",
            pron = assessment.pronScore.toPercentSmart()
                .with(ForegroundColor(assessment.pronScore.scoreToColor().first)),

            fluency = "Độ trôi chảy:\n${assessment.fluencyScore.toPercentSmart()}"
                .with(ForegroundColor(assessment.fluencyScore.scoreToColor().first))
                .with(assessment.fluencyScore.toPercentSmart(), Bold),

            accuracy = "Độ chính xác từng âm:\n${assessment.accuracyScore.toPercentSmart()}"
                .with(ForegroundColor(assessment.accuracyScore.scoreToColor().first))
                .with(assessment.accuracyScore.toPercentSmart(), Bold),

            completeness = "Độ đầy đủ câu:\n${assessment.completenessScore.toPercentSmart()}"
                .with(ForegroundColor(assessment.completenessScore.scoreToColor().first))
                .with(assessment.completenessScore.toPercentSmart(), Bold),

            pronScore = assessment.pronScore.orZero().toFloat()
        ).let {

            list.add(it)
        }

        emit(list)
    }

    val weakWordViewItem = combineSourcesWithDiff(themeFlow, assessment) {

        val theme = themeFlow.getNotNull()
        val assessment = assessment.get()

        if (assessment == null) {

            emit(emptyList())
            return@combineSourcesWithDiff
        }


        val words = assessment.nbest.flatMap {

            it.words
        }.filter {

            true
//            it.pronunciationAssessment.accuracyScore < 50
        }.sortedBy {

            it.pronunciationAssessment.accuracyScore.orZero()
        }

        if (words.isEmpty()) {

            emit(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        TextSimpleViewItem(
            id = "title",
            text = "Các từ còn yếu:".toRich(),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT
            ),
            margin = Margin(left = DP.DP_8, top = DP.DP_8, right = DP.DP_8, bottom = DP.DP_16),
        ).let {

            viewItemList.add(it)
        }

        words.map { word ->

            var text = word.word.with(ForegroundColor(theme.colorOnSurface))

            var start = 0
            word.phonemes.forEach {

                val letters = phonemeMap[it.phoneme]?.charCount ?: 1

                val end = start + letters

                text = text.with(RichRange(start, end), ForegroundColor(it.pronunciationAssessment.accuracyScore.scoreToColor().first))

                start = end
            }

            TextSimpleViewItem(
                id = word.word,
                data = word.word,
                text = text,
                margin = Margin(left = DP.DP_8, right = DP.DP_8, bottom = DP.DP_8),
                padding = Padding(paddingVertical = DP.DP_8, paddingHorizontal = DP.DP_16),
                background = Background(strokeColor = theme.colorPrimary, strokeWidth = DP.DP_1, cornerRadius = DP.DP_8)
            )
        }.let {

            viewItemList.addAll(it)
        }


        ListViewItem(
            id = "weakWordViewItem",
            viewItemList = viewItemList,
            background = Background(
                backgroundColor = theme.colorSurface,
                cornerRadius = DP.DP_16
            )
        ).let {

            emit(arrayListOf(it))
        }
    }

    val weakPhonemesViewItem = combineSourcesWithDiff(themeFlow, ipaMap, assessment) {

        val theme = themeFlow.getNotNull()

        val ipaMap = ipaMap.getNotNull()
        val assessment = assessment.get()

        if (assessment == null) {

            emit(emptyList())
            return@combineSourcesWithDiff
        }


        val phoneme = assessment.nbest.flatMap {

            it.words
        }.flatMap {

            it.phonemes
        }.filter {

            true
//            it.pronunciationAssessment.accuracyScore <= 50
        }.sortedByDescending {

            it.pronunciationAssessment.accuracyScore
        }.associateBy {

            it.phoneme
        }.values

        if (phoneme.isEmpty()) {

            emit(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        TextSimpleViewItem(
            id = "title",
            text = "Các phiên âm còn yếu:".toRich(),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT
            ),
            margin = Margin(left = DP.DP_8, top = DP.DP_8, right = DP.DP_8, bottom = DP.DP_16),
        ).let {

            viewItemList.add(it)
        }

        phoneme.map { phoneme ->

            val ipa = phonemeMap[phoneme.phoneme]?.ipa ?: phoneme.phoneme
            var text = ipa.with(ForegroundColor(phoneme.pronunciationAssessment.accuracyScore.scoreToColor().first))

            TextSimpleViewItem(
                id = phoneme.phoneme,
                data = ipaMap[ipa],
                text = text,
                margin = Margin(left = DP.DP_8, right = DP.DP_8, bottom = DP.DP_8),
                padding = Padding(paddingVertical = DP.DP_8, paddingHorizontal = DP.DP_16),
                background = Background(strokeColor = theme.colorPrimary, strokeWidth = DP.DP_1, cornerRadius = DP.DP_8)
            )
        }.let {

            viewItemList.addAll(it)
        }


        ListViewItem(
            id = "weakPhonemesViewItem",
            viewItemList = viewItemList,
            background = Background(
                backgroundColor = theme.colorSurface,
                cornerRadius = DP.DP_16
            )
        ).let {

            emit(arrayListOf(it))
        }
    }


    val viewItemList = listenerSourcesWithDiff(nBestViewItem, weakWordViewItem, weakPhonemesViewItem) {

        val list = arrayListOf<ViewItem>()

        list.add(SpaceViewItem(id = "12", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_16))
        list.addAll(nBestViewItem.value.orEmpty())
        list.add(SpaceViewItem(id = "123", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_16))
        list.addAll(weakWordViewItem.value.orEmpty())
        list.add(SpaceViewItem(id = "1234", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_16))
        list.addAll(weakPhonemesViewItem.value.orEmpty())

        emit(list)
    }


    fun updateText(it: String) {

        text.tryEmit(it)
    }

    fun stopSpeak() {

        cancel(tag = "speak")
    }

    fun startSpeak() = launchWithTag(tag = "speak") {

        channelFlow<Unit> {

            awaitClose {

                assessmentState.tryEmit(ResultState.Failed())
            }
        }.launchIn(this)

        PronunciationAssessmentUtils.recodeAsync(referenceText = text.first()).collect { state ->

            Log.d("tuanha", "startSpeak: ${state.javaClass.simpleName}  ${state.toRunning()?.data}", state.toFailed()?.cause)
            assessmentState.emit(state)
        }
    }


    private fun Double?.toPercentSmart(): String {

        val value = this.orZero()

        val percent = if (value % 1.0 == 0.0) {
            value.toInt().toString()  // Không có thập phân
        } else {
            String.format("%.2f", value).trimEnd('0').trimEnd('.')
        }

        return "$percent%"
    }

    private fun Double?.scoreToColor() = when (this.orZero()) {

        in 80.0..100.0 -> {
            Color.parseColor("#24C663") to Color.parseColor("#D8F8E4")
        }

        in 60.0..79.99 -> {
            Color.parseColor("#1867FF") to Color.parseColor("#D9E6FF")
        }

        in 30.0..59.99 -> {
            Color.parseColor("#FFB800") to Color.parseColor("#FFF3D4")
        }

        else -> {
            Color.parseColor("#FF1843") to Color.parseColor("#FFD9E0")
        }
    }

    private fun RichText.with(range: RichRange, vararg spannable: RichSpan): RichText {

        spans.add(RichStyle(range, arrayListOf(*spannable)))

        return refresh()
    }


    data class ButtonAssessmentInfo(
        val isShow: Boolean,

        val text: RichText,
        val background: Background,
    )

    private data class PhonemeConfig(
        val ipa: String,
        val charCount: Int
    )
}