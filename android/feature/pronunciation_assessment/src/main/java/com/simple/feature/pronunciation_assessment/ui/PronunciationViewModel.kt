package com.simple.feature.pronunciation_assessment.ui

import android.Manifest
import android.graphics.Color
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.toPx
import com.simple.feature.pronunciation_assessment.R
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.feature.pronunciation_assessment.domain.scoring.PhonemeTokenizer
import com.simple.feature.pronunciation_assessment.domain.usecase.PrepareAssessmentUseCase
import com.simple.feature.pronunciation_assessment.domain.usecase.StartAssessmentUseCase
import com.simple.feature.pronunciation_assessment.ui.adapters.NoteViewItem
import com.simple.feature.pronunciation_assessment.ui.adapters.ScoreResultViewItem
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.common.adapters.SpaceViewItem2
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.sp
import com.simple.state.ResultState
import com.simple.state.isFailed
import com.simple.state.isIdle
import com.simple.state.isLoading
import com.simple.state.isStart
import com.simple.state.toSuccess
import com.simple.ui.precompute.image.ColorFilter
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.node.BackgroundData
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.emptyText
import com.simple.ui.precompute.text.plus
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigRoundedBackground
import com.simple.ui.precompute.text.span.BigRoundedOutline
import com.simple.ui.precompute.text.span.BigTextSize
import com.simple.ui.precompute.text.with
import com.simple.ui.precompute.text.withFirst
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnError
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary
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
        sizes,
        themes,
        strings,
        assessment,
        initialValue = emptyList()
    ) { sizes, themes, strings, assessment ->

        fun gradeOf(score: Int): String = when {
            score >= 95 -> "GRADE A+"
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

            subtitle = subtitle.with(BigForegroundColor(themes.colorOnSurface))
                .withFirst(errorCount.toString(), BigBold, BigTextSize(16.toPx()), BigForegroundColor(themes.colorError))
                .build(),

            accuracy = assessment.accuracyScore,
            accuracyTitle = strings.getOrKey("speak_screen_result_label_accuracy")
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),
            accuracyValue = "${assessment.accuracyScore}%"
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),

            completion = assessment.completenessScore,
            completionTitle = strings.getOrKey("speak_screen_result_label_completion")
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),
            completionValue = "${assessment.completenessScore}%"
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),

            fluency = (100 - assessment.fluencyPenalty).coerceAtLeast(0),
            fluencyTitle = strings.getOrKey("speak_screen_result_label_fluency")
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),
            fluencyValue = "${(100 - assessment.fluencyPenalty).coerceAtLeast(0)}%"
                .with(BigForegroundColor(themes.colorOnSurface))
                .build(),

            maxWidth = sizes.width - 2 * DP.DP_16,
        ).let {

            viewItemList.add(SpaceViewItem2(id = "score_space_top", maxWidth = sizes.width, height = 16.dp()))
            viewItemList.add(it)
        }

        value = viewItemList
    }


    val noteViewItem: StateFlow<List<ViewItem>> = combineState(
        sizes,
        themes,
        strings,
        assessmentErrors,
        initialValue = emptyList()
    ) { sizes, themes, strings, errors ->

        val viewItemList = arrayListOf<ViewItem>()

        var noteTitle = strings.getOrKey("speak_screen_note_pronunciation_assessment")
            .with(BigTextSize(16.sp().toInt()), BigRoundedOutline(textSize = 16.toPx().toFloat(), strokeColor = Color.TRANSPARENT, paddingVertical = DP.DP_4.toFloat()), BigForegroundColor(themes.colorPrimary))
            .build()

        var note = emptyText()

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

            msg.with(BigTextSize(16.sp().toInt()), BigForegroundColor(themes.colorOnSurface))
                .withFirst("/${it.phoneme}/", BigForegroundColor(themes.colorError))
                .build()
        }.forEachIndexed { index, text ->

            if (index != 0) note += "\n"
            note += text
        }

        if (errors.isNotEmpty()) NoteViewItem(
            id = "NOTE",
            maxWidth = sizes.width - 2 * 16.dp().toInt(),

            note = note,
            title = noteTitle,
            image = R.drawable.pronunciation_assessment_ic_note_black_24dp
                .toBuilder()
                .addTransform(ColorFilter(themes.colorPrimary))
                .build(),
            background = BackgroundData(

                cornerRadius = 16.dp(),

                dashGap = 4.dp(),
                dashWidth = 4.dp(),

                strokeWidth = 1.dp(),
                strokeColor = themes.colorOnSurfaceVariant,
            )
        ).let {

            viewItemList.add(SpaceViewItem2(id = "1", maxWidth = sizes.width, height = 16.dp()))
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
                .with(BigBold, BigTextSize(16.toPx()), BigForegroundColor(textColor))
                .withFirst("Beta", BigBold, BigRoundedBackground(backgroundColor = themes.colorError, themes.colorOnError, DP.DP_4.toFloat()))
        } else if (initState is ResultState.Running && initState.data in 0..99) {
            strings.getOrKey("speak_screen_action_loading_model")
                .replace("\$percent", "${initState.data}%")
                .with(BigBold, BigTextSize(16.toPx()), BigForegroundColor(textColor))
                .withFirst("${initState.data}%", BigForegroundColor(themes.colorError))
        } else if (initState.isLoading()) {
            strings.getOrKey("speak_screen_action_loading_ai_model")
                .with(BigBold, BigTextSize(16.toPx()), BigForegroundColor(textColor))
        } else if (assessmentState.isStart()) {
            strings.getOrKey("speak_screen_action_assessing")
                .with(BigBold, BigTextSize(16.toPx()), BigForegroundColor(textColor))
        } else {
            strings.getOrKey("speak_screen_action_practice")
                .with(BigBold, BigTextSize(16.toPx()), BigForegroundColor(textColor))
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
            text = text.build(),
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
        val text: BigText = emptyText(),
        val textShow: Boolean = true,
        val imageShow: Boolean = false,
        val progressShow: Boolean = false,

        val loading: Boolean = false,
        val background: Background = com.simple.coreapp.ui.view.Background(),
    )
}
