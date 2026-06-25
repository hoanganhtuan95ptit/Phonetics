package com.simple.feature.pronunciation_assessment

import android.Manifest
import android.graphics.Color
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.feature.pronunciation_assessment.ui.adapters.NoteViewItem
import com.simple.feature.pronunciation_assessment.ui.adapters.ScoreResultViewItem
import com.simple.feature.pronunciation_assessment.use_case.PronunciationPipeline
import com.simple.feature.pronunciation_assessment.utils.plus
import com.simple.image.ImageRes
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.spans.TextSize
import com.simple.state.ResultState
import com.simple.state.isIdea
import com.simple.state.isLoading
import com.simple.state.isStart
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import com.unknown.theme.utils.exts.colorSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class PronunciationViewModel : BaseViewModel() {

    private val pronunciationPipeline by lazy {
        PronunciationPipeline(PhoneticsApp.share)
    }

    val initState: MutableStateFlow<ResultState<Int>> = MutableStateFlow(ResultState.IDEA)

    val recordState: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.IDEA)

    val assessmentState: MutableStateFlow<ResultState<SentenceScore>> = MutableStateFlow(ResultState.IDEA) // todo fake


    val resultViewItem: StateFlow<List<ViewItem>> = combineState(
        themes,
        strings,
        assessmentState.mapNotNull { it.toSuccess()?.data },
        emptyList()
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
            assessment.finalScore >= 70 -> strings.getOrKey("speak_screen_result_subtitle_70").replace("\$error_count", errorCount.toString())
            assessment.finalScore >= 50 -> strings.getOrKey("speak_screen_result_subtitle_50").replace("\$error_count", errorCount.toString())
            else -> strings.getOrKey("speak_screen_result_subtitle_0").replace("\$error_count", errorCount.toString())
        }

        ScoreResultViewItem(
            id = "score_result",
            score = assessment.finalScore,
            label = strings.getOrKey("speak_screen_result_label_score"),
            grade = gradeOf(assessment.finalScore),

            subtitle = subtitle.with(ForegroundColor(themes.colorOnSurface))
                .with(errorCount.toString(), ForegroundColor(themes.colorError)),

            accuracy = assessment.accuracyScore,
            accuracyTitle = strings.getOrKey("speak_screen_result_label_accuracy").with(ForegroundColor(themes.colorOnSurface)),
            accuracyValue = "${assessment.accuracyScore}%".with(ForegroundColor(themes.colorOnSurface)),

            completion = assessment.completenessScore,
            completionTitle = strings.getOrKey("speak_screen_result_label_completion").with(ForegroundColor(themes.colorOnSurface)),
            completionValue = "${assessment.completenessScore}%".with(ForegroundColor(themes.colorOnSurface)),

            fluency = (100 - assessment.fluencyPenalty).coerceAtLeast(0),
            fluencyTitle = strings.getOrKey("speak_screen_result_label_fluency").with(ForegroundColor(themes.colorOnSurface)),
            fluencyValue = "${(100 - assessment.fluencyPenalty).coerceAtLeast(0)}%".with(ForegroundColor(themes.colorOnSurface)),
        ).let {

            viewItemList.add(SpaceViewItem(id = "score_space_top", height = DP.DP_16))
            viewItemList.add(it)
        }

        viewItemList
    }


    val noteViewItem: StateFlow<List<ViewItem>> = combineState(
        themes,
        strings,
        assessmentState.mapNotNull { it.toSuccess()?.data?.errors },
        emptyList()
    ) { themes, strings, errors ->

        val viewItemList = arrayListOf<ViewItem>()

        var title = strings.getOrKey("speak_screen_note_pronunciation_assessment")
            .with(TextSize(16), ForegroundColor(themes.colorPrimary))

        errors.mapIndexedNotNull { index, it ->

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

                else -> return@mapIndexedNotNull null
            }

            msg.with("/${it.phoneme}/", ForegroundColor(themes.colorError))
        }.forEach {

            title += it
        }

        NoteViewItem(
            id = "NOTE",
            note = title,
            image = ImageRes(R.drawable.pronunciation_assessment_ic_note_black_24dp, themes.colorPrimary),
            background = Background(
                cornerRadius = DP.DP_16,

                strokeWidth = DP.DP_1,
                strokeColor = themes.colorSurfaceVariant
            )
        ).let {

            viewItemList.add(SpaceViewItem(id = "1", height = DP.DP_16))
            viewItemList.add(it)
        }

        viewItemList
    }


    val buttonData: StateFlow<ButtonData> = combineState(
        themes,
        strings,
        initState,
        recordState,
        assessmentState,
        ButtonData()
    ) { themes, strings, initState, recordState, assessmentState ->

        val textColor = if (!initState.isLoading() && !assessmentState.isLoading()) {
            themes.colorOnPrimary
        } else {
            themes.colorPrimary
        }

        val text = if (initState.isIdea()) {
            strings.getOrKey("speak_screen_action_pronunciation_assessment")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else if (initState is ResultState.Running && initState.data in 0..99) {
            strings.getOrKey("speak_screen_loading_model")
                .replace("\$percent", "${initState.data}%")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
                .with("${initState.data}%", ForegroundColor(themes.colorError))
        } else if (initState.isLoading()) {
            strings.getOrKey("speak_screen_loading_ai_model")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else if (assessmentState.isStart()) {
            strings.getOrKey("speak_screen_assessing")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else {
            strings.getOrKey("speak_screen_practice")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        }


        val backgroundColor = if (initState.isIdea()) {
            themes.colorPrimary
        } else if (initState.isLoading() || recordState.isLoading() || assessmentState.isLoading()) {
            Color.TRANSPARENT
        } else {
            themes.colorPrimary
        }

        val imageShow = recordState.isLoading()

        ButtonData(
            text = text,
            textShow = !imageShow,

            imageShow = imageShow,

            progressShow = initState is ResultState.Running && initState.data in 0..99,

            loading = initState.isLoading() || assessmentState.isLoading(),

            background = Background(
                cornerRadius = DP.DP_10,
                backgroundColor = backgroundColor
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        pronunciationPipeline.close()
    }

    fun loadModel(sentences: List<Sentence>) = viewModelScope.launch(handler + Dispatchers.IO) {

        initState.value = ResultState.Start

        val reference = sentences.flatMap {

            it.phonetics
        }.map { phonetic ->

            phonetic.text to phonetic.ipaValueList.firstOrNull().orEmpty().parseIpaPhonemes()
        }

        pronunciationPipeline.prepare(reference = reference, useGPU = true) {

            initState.value = (ResultState.Running(it))
        }

        initState.value = (ResultState.Success(100))
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(): Job = launchWithTag("record") {

        channelFlow<Unit> {

            recordState.value = (ResultState.Start)

            pronunciationPipeline.onPartialResult = { score ->
            }

            pronunciationPipeline.onRecordEnd = {
                assessmentState.value = (ResultState.Start)
                launch {
                    delay(100)
                    recordState.value = (ResultState.Success(""))
                }
            }
            pronunciationPipeline.onFinalResult = { score ->
                assessmentState.value = (ResultState.Success(score))
            }
            pronunciationPipeline.onStateChange = { state ->
            }

            pronunciationPipeline.onError = { msg ->
                recordState.value = (ResultState.Failed(AppException(msg)))
            }

            pronunciationPipeline.startListening()

            awaitClose {
                if (recordState.value.isLoading()) recordState.value = (ResultState.Failed(AppException("")))
                pronunciationPipeline.stopListening()
            }
        }.collect()
    }

    /**
     * Tách chuỗi IPA (vd: "/ˈtiːm/") thành list phoneme riêng lẻ (vd: ["t","iː","m"]).
     * Dùng greedy matching để ưu tiên token nhiều ký tự trước (iː, tʃ, ...).
     */
    private fun String.parseIpaPhonemes(): List<String> {
        val clean = replace("/", "").replace("ˈ", "").replace("ˌ", "").trim()
        val multiChar = listOf("tʃ", "dʒ", "iː", "uː", "eɪ", "ɜː", "ɔː", "aɪ", "aʊ", "ɔɪ", "oʊ", "ɑː")
        val result = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val matched = multiChar.firstOrNull { clean.startsWith(it, i) }
            if (matched != null) {
                result.add(matched)
                i += matched.length
            } else {
                val ch = clean[i].toString()
                if (ch.isNotBlank()) result.add(ch)
                i++
            }
        }
        return result
    }

    data class ButtonData(
        val text: RichText = emptyText(),
        val textShow: Boolean = true,
        val imageShow: Boolean = false,
        val progressShow: Boolean = false,

        val loading: Boolean = false,
        val background: Background = com.simple.coreapp.ui.view.Background()
    )
}