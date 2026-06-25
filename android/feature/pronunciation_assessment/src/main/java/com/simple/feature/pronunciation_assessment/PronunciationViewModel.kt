package com.simple.feature.pronunciation_assessment

import android.Manifest
import android.graphics.Color
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.feature.pronunciation_assessment.use_case.PronunciationPipeline
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.phonetics.utils.spans.TextSize
import com.simple.state.ResultState
import com.simple.state.isIdea
import com.simple.state.isLoading
import com.simple.state.isStart
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PronunciationViewModel : BaseViewModel() {

    private val pronunciationPipeline by lazy {
        PronunciationPipeline(PhoneticsApp.share)
    }

    val initState: MutableStateFlow<ResultState<Int>> = MutableStateFlow(ResultState.IDEA)

    val recordState: MutableStateFlow<ResultState<String>> = MutableStateFlow(ResultState.IDEA)

    val assessmentState: MutableStateFlow<ResultState<SentenceScore>> = MutableStateFlow(ResultState.IDEA)


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
            strings.getOrEmpty("speak_screen_action_pronunciation_assessment")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else if (initState is ResultState.Running && initState.data in 0..99) {
            strings.getOrEmpty("speak_screen_loading_model")
                .replace("\$percent", "${initState.data}%")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
                .with("${initState.data}%", ForegroundColor(themes.colorError))
        } else if (initState.isLoading()) {
            strings.getOrEmpty("speak_screen_loading_ai_model")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else if (assessmentState.isStart()) {
            strings.getOrEmpty("speak_screen_assessing")
                .with(Bold, TextSize(16), ForegroundColor(textColor))
        } else {
            strings.getOrEmpty("speak_screen_practice")
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