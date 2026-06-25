package com.simple.phonetics.ui.speak.services.pronunciation_assessment

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.orZero
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetic.entities.ipaValueList
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case.PronunciationPipeline
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case.SentenceScore
import com.simple.state.ResultState
import com.simple.state.isIdea
import com.simple.state.isLoading
import com.simple.state.isStart
import com.simple.state.toRunning
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PronunciationViewModel : BaseViewModel() {

    private val fakeState by lazy {

        val sentenceScore = """
            {"accuracyScore":51,"completenessScore":53,"errors":[{"errorType":"DELETION","phoneme":"ð","wordContext":"the"},{"errorType":"SUBSTITUTION","phoneme":"ə","substitutedWith":"i","wordContext":"the"},{"errorType":"SUBSTITUTION","phoneme":"t","substitutedWith":"s","wordContext":"cat"},{"errorType":"SUBSTITUTION","phoneme":"s","substitutedWith":"e","wordContext":"sat"},{"errorType":"SUBSTITUTION","phoneme":"æ","substitutedWith":"o","wordContext":"sat"},{"errorType":"SUBSTITUTION","phoneme":"t","substitutedWith":"n","wordContext":"sat"}],"finalScore":51,"referenceText":"the cat sat on the mat","wordScores":[{"phonemeScores":[{"errorType":"DELETION","expected":"ð"},{"actual":"i","errorType":"SUBSTITUTION","expected":"ə","score":35}],"score":17,"word":"the"},{"phonemeScores":[{"actual":"k","errorType":"CORRECT","expected":"k","score":90},{"actual":"æ","errorType":"CORRECT","expected":"æ","score":90},{"actual":"s","errorType":"SUBSTITUTION","expected":"t","score":70}],"score":83,"word":"cat"},{"phonemeScores":[{"actual":"e","errorType":"SUBSTITUTION","expected":"s","score":35},{"actual":"o","errorType":"SUBSTITUTION","expected":"æ","score":35},{"actual":"n","errorType":"SUBSTITUTION","expected":"t","score":55}],"score":41,"word":"sat"}],"partial":true}
        """.trimIndent()

        ResultState.Success(sentenceScore.toObject<SentenceScore>())
    }

    private val pronunciationPipeline by lazy {
        PronunciationPipeline(PhoneticsApp.share)
    }

    val initState: LiveData<ResultState<Int>> = MediatorLiveData(ResultState.IDEA)

    val recordState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.IDEA)

    val assessmentState: LiveData<ResultState<SentenceScore>> = MediatorLiveData(ResultState.IDEA)


    val buttonData: LiveData<ButtonData> = combineSourcesWithDiff<ButtonData>(translate, theme, initState, recordState, assessmentState) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val initState = initState.value ?: return@combineSourcesWithDiff
        val recordState = recordState.value ?: return@combineSourcesWithDiff
        val assessmentState = assessmentState.value ?: return@combineSourcesWithDiff

        val text = if (initState.isIdea()) {
            "Chấm điểm phát âm"
        } else if (initState is ResultState.Running && initState.data in 0..99) {
            "Đang tải model ${initState.data}%"
        } else if (initState.isLoading()) {
            "Đang load model AI"
        } else if (assessmentState.isStart()) {
            "Đang chấm điểm"
        } else {
            "Luyện phát âm"
        }

        val textColor = if (!initState.isLoading() && !assessmentState.isLoading()) {
            theme.colorOnPrimary
        } else {
            theme.colorPrimary
        }

        val backgroundColor = if (initState.isIdea()) {
            theme.colorPrimary
        } else if (initState.isLoading() || recordState.isLoading() || assessmentState.isLoading()) {
            Color.TRANSPARENT
        } else {
            theme.colorPrimary
        }

        val imageShow = recordState.isLoading()

        ButtonData(
            text = text.with(ForegroundColor(textColor)),
            textShow = !imageShow,

            imageShow = imageShow,

            progressShow = initState is ResultState.Running && initState.data in 0..99,

            loading = initState.isLoading() || assessmentState.isLoading(),

            background = com.simple.coreapp.ui.view.Background(
                cornerRadius = DP.DP_10,
                backgroundColor = backgroundColor
            )
        ).let {

            postValue(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pronunciationPipeline.close()
    }

    fun loadModel(sentences: List<Sentence>) = viewModelScope.launch(handler + Dispatchers.IO) {

        initState.postValue(ResultState.Start)

        val reference = sentences.flatMap {

            it.phonetics
        }.map { phonetic ->

            phonetic.text to phonetic.ipaValueList.firstOrNull().orEmpty().parseIpaPhonemes()
        }

        pronunciationPipeline.prepare(reference = reference, useGPU = true) {

            initState.postValue(ResultState.Running(it))
        }

        initState.postValue(ResultState.Success(100))
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun record(): Job = launchWithTag("record") {

        channelFlow<Unit> {

            recordState.postValue(ResultState.Start)

            pronunciationPipeline.onPartialResult = { score ->
//                pronunciationRecordState.postValue(ResultState.Running(score))
            }

            pronunciationPipeline.onRecordEnd = {
                assessmentState.postValue(ResultState.Start)
                launch {
                    delay(100)
                    recordState.postValue(ResultState.Success(""))
                }
//                pronunciationRecordState.postValue(ResultState.Running(score))
            }
            pronunciationPipeline.onFinalResult = { score ->
                assessmentState.postValue(ResultState.Success(score))
            }
            pronunciationPipeline.onStateChange = { state ->
//                statusText.text = when (state) {
//                    PipelineState.READY -> "Sẵn sàng"
//                    PipelineState.LISTENING -> "Đang nghe..."
//                    PipelineState.RECORDING -> "Đang ghi âm"
//                    PipelineState.PROCESSING -> "Đang xử lý..."
//                    PipelineState.ERROR -> "Lỗi"
//                    else -> "Đang tải model..."
//                }
//                micButton.text = when (state) {
//                    PipelineState.RECORDING, PipelineState.LISTENING -> "Dừng lại"
//                    else -> "Bắt đầu nói"
//                }
            }

            pronunciationPipeline.onError = { msg ->
                recordState.postValue(ResultState.Failed(AppException(msg)))
            }

            pronunciationPipeline.startListening()

            awaitClose {
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
        val text: RichText,
        val textShow: Boolean = true,
        val imageShow: Boolean = false,
        val progressShow: Boolean = false,

        val loading: Boolean,
        val background: com.simple.coreapp.ui.view.Background
    )
}