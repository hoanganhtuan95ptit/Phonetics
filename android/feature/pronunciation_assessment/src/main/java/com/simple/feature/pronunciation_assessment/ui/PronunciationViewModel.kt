package com.simple.feature.pronunciation_assessment.ui

import android.Manifest
import android.graphics.Color
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.toPx
import com.simple.feature.pronunciation_assessment.R
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.feature.pronunciation_assessment.domain.scoring.PhonemeTokenizer
import com.simple.feature.pronunciation_assessment.domain.usecase.PrepareAssessmentUseCase
import com.simple.feature.pronunciation_assessment.domain.usecase.StartAssessmentUseCase
import com.simple.feature.pronunciation_assessment.ui.adapters.NoteViewItem
import com.simple.feature.pronunciation_assessment.ui.adapters.ScoreResultViewItem
import com.simple.feature.pronunciation_assessment.utils.plus
import com.simple.image.ImageRes
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.spans.RoundedBackground
import com.simple.phonetics.utils.spans.RoundedOutline
import com.simple.phonetics.utils.spans.TextSize
import com.simple.state.ResultState
import com.simple.state.isFailed
import com.simple.state.isIdle
import com.simple.state.isLoading
import com.simple.state.isStart
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnError
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import com.unknown.theme.utils.exts.colorSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class PronunciationViewModel : BaseViewModel() {

    // ── UI state ──────────────────────────────
    val initState: MutableStateFlow<ResultState<Int>> = MutableStateFlow(ResultState.Idle)

    val recordState: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.Idle)

    val assessmentState: MutableStateFlow<ResultState<SentenceScore>> = MutableStateFlow(ResultState.Idle)


    val assessment = assessmentState.mapNotNull {
        it.toSuccess()?.data
    }

    val assessmentErrors = assessment.map {
        it.errors
    }

    val resultViewItem: StateFlow<List<ViewItem>> = combineState(
        themes,
        strings,
        assessment,
        initialValue = emptyList()
    ) { themes, strings, assessment ->

        fun gradeOf(score: Int): String = when {
            score >= 90 -> "GRADE A"
            score >= 80 -> "GRADE B+"
            score >= 70 -> "GRADE B"
            score >= 60 -> "GRADE C+"
            score >= 50 -> "GRADE C"
            else -> "GRADE D"
        }


        val viewItemList = arrayListOf<ViewItem>()

        val errorCount = assessment.errors.size

        val subtitle = when {
            assessment.finalScore >= 90 -> strings.getOrKey("speak_screen_result_subtitle_90")
            assessment.finalScore >= 70 -> strings.getOrKey("speak_screen_result_subtitle_70")
                .replace("\$error_count", errorCount.toString())

            assessment.finalScore >= 50 -> strings.getOrKey("speak_screen_result_subtitle_50")
                .replace("\$error_count", errorCount.toString())

            else -> strings.getOrKey("speak_screen_result_subtitle_0")
                .replace("\$error_count", errorCount.toString())
        }

        ScoreResultViewItem(
            id = "score_result",
            score = assessment.finalScore,
            label = strings.getOrKey("speak_screen_result_label_score"),
            grade = gradeOf(assessment.finalScore),

            subtitle = subtitle.with(ForegroundColor(themes.colorOnSurface))
                .with(errorCount.toString(), Bold, TextSize(16), ForegroundColor(themes.colorError)),

            accuracy = assessment.accuracyScore,
            accuracyTitle = strings.getOrKey("speak_screen_result_label_accuracy")
                .with(ForegroundColor(themes.colorOnSurface)),
            accuracyValue = "${assessment.accuracyScore}%"
                .with(ForegroundColor(themes.colorOnSurface)),

            completion = assessment.completenessScore,
            completionTitle = strings.getOrKey("speak_screen_result_label_completion")
                .with(ForegroundColor(themes.colorOnSurface)),
            completionValue = "${assessment.completenessScore}%"
                .with(ForegroundColor(themes.colorOnSurface)),

            fluency = (100 - assessment.fluencyPenalty).coerceAtLeast(0),
            fluencyTitle = strings.getOrKey("speak_screen_result_label_fluency")
                .with(ForegroundColor(themes.colorOnSurface)),
            fluencyValue = "${(100 - assessment.fluencyPenalty).coerceAtLeast(0)}%"
                .with(ForegroundColor(themes.colorOnSurface)),
        ).let {

            viewItemList.add(SpaceViewItem(id = "score_space_top", height = DP.DP_16))
            viewItemList.add(it)
        }

        value = viewItemList
    }


    val noteViewItem: StateFlow<List<ViewItem>> = combineState(
        themes,
        strings,
        assessmentErrors,
        initialValue = emptyList()
    ) { themes, strings, errors ->

        val viewItemList = arrayListOf<ViewItem>()

        var note = strings.getOrKey("speak_screen_note_pronunciation_assessment")
            .with(RoundedOutline(textSize = 16.toPx().toFloat(), strokeColor = Color.TRANSPARENT, paddingVertical = DP.DP_4.toFloat()), ForegroundColor(themes.colorPrimary))

        errors.mapNotNull {

            val msg = when (it.errorType) {
                ErrorType.SUBSTITUTION -> strings.getOrKey("speak_screen_note_substitution")
                    .replace("\$phoneme", it.phoneme)
                    .replace("\$substitutedWith", it.substitutedWith.orEmpty())
                    .replace("\$wordContext", it.wordContext)

                ErrorType.INSERTION -> strings.getOrKey("speak_screen_note_insertion")
                    .replace("\$phoneme", it.phoneme)

                ErrorType.DELETION -> strings.getOrKey("speak_screen_note_deletion")
                    .replace("\$phoneme", it.phoneme)
                    .replace("\$wordContext", it.wordContext)

                else -> return@mapNotNull null
            }

            msg.with(ForegroundColor(themes.colorOnSurface))
                .with("/${it.phoneme}/", ForegroundColor(themes.colorError))
        }.forEach {

            note += "\n"
            note += it
        }

        if (errors.isNotEmpty()) NoteViewItem(
            id = "NOTE",
            note = note,
            image = ImageRes(
                R.drawable.pronunciation_assessment_ic_note_black_24dp,
                themes.colorPrimary
            ),
            background = Background(
                cornerRadius = DP.DP_16,
                strokeWidth = DP.DP_1,
                strokeColor = themes.colorSurfaceVariant,
            )
        ).let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.add(it)
        }

        value = viewItemList
    }


    val buttonData: StateFlow<ButtonData> = combineState(
        themes,
        strings,
        initState,
        recordState,
        assessmentState,
        initialValue = ButtonData()
    ) { themes, strings, initState, recordState, assessmentState ->

        val textColor = if (!initState.isLoading() && !assessmentState.isLoading()) {
            themes.colorOnPrimary
        } else {
            themes.colorPrimary
        }

        val text = if (initState.isIdle() || initState.isFailed()) {
            (strings.getOrKey("speak_screen_action_pronunciation_assessment") + " Beta")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
                .with("Beta", Bold, RoundedBackground(backgroundColor = themes.colorError, themes.colorOnError, DP.DP_4.toFloat()))
        } else if (initState is ResultState.Running && initState.data in 0..99) {
            strings.getOrKey("speak_screen_action_loading_model")
                .replace("\$percent", "${initState.data}%")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
                .with("${initState.data}%", ForegroundColor(themes.colorError))
        } else if (initState.isLoading()) {
            strings.getOrKey("speak_screen_action_loading_ai_model")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else if (assessmentState.isStart()) {
            strings.getOrKey("speak_screen_action_assessing")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else {
            strings.getOrKey("speak_screen_action_practice")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        }


        val backgroundColor = if (initState.isIdle()) {
            themes.colorPrimary
        } else if (initState.isLoading() || recordState.isLoading() || assessmentState.isLoading()) {
            Color.TRANSPARENT
        } else {
            themes.colorPrimary
        }

        val imageShow = recordState.isLoading()

        value = ButtonData(
            text = text,
            textShow = !imageShow,

            imageShow = imageShow,

            progressShow = initState is ResultState.Running && initState.data in 0..99,

            loading = initState.isLoading() || assessmentState.isLoading(),

            background = Background(
                cornerRadius = DP.DP_10,
                backgroundColor = backgroundColor,
            )
        )
    }

    fun loadModel() = viewModelScope.launch(handler + Dispatchers.IO) {

        initState.value = ResultState.Start

        val param = PrepareAssessmentUseCase.Param(
            useGPU = true,
        )

        PrepareAssessmentUseCase.instance.execute(param).collect {

            initState.value = it
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(sentences: List<Sentence>): Job = launchWithTag("record") {

        recordState.value = ResultState.Start

        val referenceWords = sentences.flatMap {

            it.phonetics
        }.map { phonetic ->

            phonetic.text to PhonemeTokenizer.parseIpa(phonetic.ipaValueList.firstOrNull().orEmpty())
        }

        StartAssessmentUseCase.instance.execute(referenceWords).collect { event ->

            when (event) {
                is AssessmentEvent.StateChanged -> {
                    // pipeline state — UI hiện không bind, để dành mở rộng sau
                }

                is AssessmentEvent.Partial -> {
                    // partial — UI hiện chưa bind, có thể thêm sau
                }

                is AssessmentEvent.RecordEnded -> {
                    assessmentState.value = ResultState.Start
                    delay(100)
                    recordState.value = ResultState.Success("")
                }

                is AssessmentEvent.Final -> {
                    assessmentState.value = ResultState.Success(event.score)
                }

                is AssessmentEvent.Error -> {
                    recordState.value = ResultState.Failed(AppException(event.message))
                }
            }
        }
    }

    data class ButtonData(
        val text: RichText = emptyText(),
        val textShow: Boolean = true,
        val imageShow: Boolean = false,
        val progressShow: Boolean = false,

        val loading: Boolean = false,
        val background: Background = com.simple.coreapp.ui.view.Background(),
    )
}
