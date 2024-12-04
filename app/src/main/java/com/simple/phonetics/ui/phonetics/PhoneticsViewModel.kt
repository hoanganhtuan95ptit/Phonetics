package com.simple.phonetics.ui.phonetics

import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.LoadingViewItem
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.hasChar
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toPx
import com.simple.detect.data.usecase.DetectUseCase
import com.simple.detect.entities.DetectOption
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.StartSpeakUseCase
import com.simple.phonetics.domain.usecase.language.StopSpeakUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.adapters.TitleViewItem
import com.simple.phonetics.ui.base.TransitionViewModel
import com.simple.phonetics.ui.phonetics.adapters.EmptyViewItem
import com.simple.phonetics.ui.phonetics.adapters.HistoryViewItem
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsViewItem
import com.simple.phonetics.ui.phonetics.adapters.SentenceViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.exts.with
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.toRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PhoneticsViewModel(
    private val detectUseCase: DetectUseCase,
    private val stopSpeakUseCase: StopSpeakUseCase,
    private val startSpeakUseCase: StartSpeakUseCase,
    private val detectStateUseCase: DetectStateUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase,
    private val getKeyTranslateAsyncUseCase: GetKeyTranslateAsyncUseCase,
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : TransitionViewModel() {

    private val itemLoading = listOf(
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading),
        LoadingViewItem(R.layout.item_phonetics_loading)
    )

    @VisibleForTesting
    val theme: LiveData<AppTheme> = mediatorLiveData {

        appTheme.collect {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val keyTranslateMap: LiveData<Map<String, String>> = mediatorLiveData {

        getKeyTranslateAsyncUseCase.execute().collect {

            postDifferentValue(it)
        }
    }

    val title: LiveData<CharSequence> = combineSources(theme, keyTranslateMap) {

        val theme = theme.get()
        val keyTranslateMap = keyTranslateMap.getOrEmpty()

        val title = keyTranslateMap["Ephonetics"].orEmpty().with("Ep", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.primaryColor))

        postDifferentValue(title)
    }


    @VisibleForTesting
    val inputLanguage: LiveData<Language> = MediatorLiveData()

    @VisibleForTesting
    val outputLanguage: LiveData<Language> = MediatorLiveData()


    @VisibleForTesting
    val historyViewItemList: LiveData<List<HistoryViewItem>> = mediatorLiveData {

        getPhoneticsHistoryAsyncUseCase.execute(null).collect { list ->

            list.mapIndexed { index, sentence ->

                HistoryViewItem(
                    id = sentence.text,
                    text = sentence.text,
                    isLast = index == list.lastIndex
                )
            }.let {

                postDifferentValue(it)
            }
        }
    }


    @VisibleForTesting
    val text: LiveData<String> = MediatorLiveData("")


    val detectState: LiveData<ResultState<String>> = MediatorLiveData()

    @VisibleForTesting
    val isSupportDetect: LiveData<Boolean> = combineSources(inputLanguage) {

        val inputLanguage = inputLanguage.value ?: return@combineSources

        postValue(false)

        val isSupported = detectStateUseCase.execute(DetectStateUseCase.Param(inputLanguage.id))

        postValue(isSupported)
    }

    val imageInfo: LiveData<ImageInfo> = combineSources(detectState, isSupportDetect) {

        val detectState = detectState.get()
        val isSupportDetect = isSupportDetect.get()

        val info = ImageInfo(
            image = detectState.toRunning()?.data.orEmpty(),
            isShowImage = !detectState.isCompleted(),
            isShowInput = isSupportDetect
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val isSupportReverse: LiveData<Boolean> = MediatorLiveData(true)

    val reverseInfo: LiveData<ReverseInfo> = combineSources(isReverse, isSupportReverse, keyTranslateMap) {

        val isReverse = isReverse.get()
        val keyTranslateMap = keyTranslateMap.get()
        val isSupportReverse = isSupportReverse.get()

        val info = ReverseInfo(
            text = keyTranslateMap["action_reverse"].orEmpty(),
            isShow = isSupportReverse,
            isSelected = isReverse
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val speakState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Success(""))

    @VisibleForTesting
    val isSupportSpeak: LiveData<Boolean> = MediatorLiveData(true)

    val speakInfo: LiveData<SpeakInfo> = combineSources(text, speakState, isSupportSpeak) {

        val text = text.get()
        val speakState = speakState.get()
        val isSupportSpeak = isSupportSpeak.get() && text.isNotBlank()

        val info = SpeakInfo(
            isShowPlay = !speakState.isRunning() && isSupportSpeak,
            isShowPause = speakState.isRunning() && isSupportSpeak
        )

        postDifferentValue(info)
    }


    val clearInfo: LiveData<ClearInfo> = combineSources(text, keyTranslateMap) {

        val text = text.get()
        val keyTranslateMap = keyTranslateMap.get()

        val info = ClearInfo(
            text = keyTranslateMap["action_clear"].orEmpty(),
            isShow = text.isNotBlank()
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val phoneticsCode: LiveData<String> = MediatorLiveData()

    @VisibleForTesting
    val isSupportTranslate: LiveData<Boolean> = MediatorLiveData()


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(text, isReverse, inputLanguage, outputLanguage) {

        val inputLanguageCode = inputLanguage.get().id

        val outputLanguageCode = outputLanguage.get().id

        getPhoneticsAsyncUseCase.execute(GetPhoneticsAsyncUseCase.Param(text.get(), isReverse.get(), inputLanguageCode, outputLanguageCode)).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticsViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(phoneticsCode, phoneticsState, isSupportTranslate, keyTranslateMap) {

        val state = phoneticsState.get()
        val phoneticsCode = phoneticsCode.get()
        val keyTranslateMap = keyTranslateMap.get()
        val isSupportTranslate = isSupportTranslate.get()


        state.doStart {

            postDifferentValue(itemLoading)
            return@combineSources
        }


        val listItem = state.toSuccess()?.data.orEmpty()

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            if (item is Phonetics) item.let { phonetic ->

                val codeAndIpa = phonetic.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }

                val ipa = (codeAndIpa?.get(phoneticsCode) ?: codeAndIpa?.toList()?.first()?.second)?.firstOrNull().orEmpty()

                PhoneticsViewItem(
                    id = "${indexItem * 1000}",
                    data = phonetic,

                    ipa = ipa,
                    text = phonetic.text,

                    isMultiIpa = phonetic.ipa.size > 1
                )
            }.let {

                return@flatMapIndexed listOf(it)
            }


            if (item !is Sentence) {

                return@flatMapIndexed emptyList()
            }


            val list = arrayListOf<ViewItem>()

            item.phonetics.mapIndexed { indexPhonetic, phonetic ->

                val codeAndIpa = phonetic.ipa.filter { it.value.isNotEmpty() }.takeIf { it.isNotEmpty() }

                val ipa = (codeAndIpa?.get(phoneticsCode) ?: codeAndIpa?.toList()?.first()?.second)?.firstOrNull().orEmpty()

                PhoneticsViewItem(
                    id = "${indexItem * 1000 + indexPhonetic}",
                    data = phonetic,

                    ipa = ipa,
                    text = phonetic.text,

                    isMultiIpa = phonetic.ipa.size > 1
                )
            }.let {

                list.addAll(it)
            }

            if (isSupportTranslate && item.text.hasChar()) item.translateState.let { translateState ->

                val text = if (translateState is ResultState.Start) {
                    keyTranslateMap["translating"].orEmpty()
                } else if (translateState is ResultState.Success) {
                    translateState.data
                } else {
                    keyTranslateMap["translate_failed"].orEmpty()
                }

                SentenceViewItem(
                    "${indexItem * 1000}",
                    item,
                    text = text,
                    isLast = indexItem == listItem.lastIndex
                )
            }.let {

                list.add(it)
            }

            list
        }.let {

            val list = it.toMutableList()

            list.add(SpaceViewItem(height = DP.DP_100))

            postDifferentValue(list)
        }
    }.apply {

        postDifferentValue(emptyList())
    }

    val listViewItem: LiveData<List<ViewItem>> = combineSources(text, theme, keyTranslateMap, historyViewItemList, phoneticsViewItemList) {

        val text = text.get()
        val theme = theme.get()
        val keyTranslateMap = keyTranslateMap.get()
        val historyViewItemList = historyViewItemList.getOrEmpty()
        val phoneticsViewItemList = phoneticsViewItemList.getOrEmpty()

        val viewItemList = arrayListOf<ViewItem>()

        if (historyViewItemList.isNotEmpty() && text.isBlank()) TitleViewItem(
            text = keyTranslateMap["title_history"].orEmpty(),
            textSize = 20f,
            textColor = theme.primaryColor,
            paddingHorizontal = DP.DP_16
        ).let {

            viewItemList.add(it)
        }

        if (text.isNotBlank()) {

            viewItemList.addAll(phoneticsViewItemList)
        } else {

            viewItemList.addAll(historyViewItemList)
        }

        if (viewItemList.isNotEmpty()) {

            viewItemList.add(SpaceViewItem(height = 60.toPx()))
        } else EmptyViewItem(
            id = "EMPTY",
            message = keyTranslateMap["message_result_empty"].orEmpty(),
            imageRes = R.raw.anim_empty
        ).let {

            viewItemList.add(it)
        }

        postDifferentValue(viewItemList)
    }


    val hintEnter: LiveData<String> = combineSources(isReverse, outputLanguage, keyTranslateMap) {

        val outputLanguage = outputLanguage.value ?: return@combineSources
        val keyTranslateMap = keyTranslateMap.value ?: return@combineSources

        val hint = if (isReverse.value == true) {
            keyTranslateMap["hint_enter_language_text"].orEmpty().replace("\$language_name", outputLanguage.name)
        } else {
            keyTranslateMap["hint_enter_text"].orEmpty()
        }

        postDifferentValue(hint)
    }

    val isShowLoading: LiveData<Boolean> = combineSources(speakState, detectState) {

        postDifferentValue(speakState.value.isStart() || !detectState.value.isCompleted())
    }

    fun getPhonetics(text: String) {

        this.text.postDifferentValue(text)
    }

    fun switchReverse() {

        this.isReverse.postValue(!this.isReverse.get())
    }

    fun updateSupportSpeak(b: Boolean) {

        this.isSupportSpeak.postDifferentValue(b)
    }

    fun updatePhoneticSelect(code: String) {

        this.phoneticsCode.postDifferentValue(code)
    }

    fun updateSupportTranslate(b: Boolean) {

        this.isSupportReverse.postDifferentValue(b)
        this.isSupportTranslate.postDifferentValue(b)
    }

    fun updateInputLanguage(language: Language) {

        this.inputLanguage.postDifferentValue(language)
    }

    fun updateOutputLanguage(language: Language) {

        this.outputLanguage.postDifferentValue(language)
    }


    fun startSpeak(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        speakState.postValue(ResultState.Start)

        val param = StartSpeakUseCase.Param(
            text = text,

            languageCode = languageCode,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

        var job: Job? = null

        job = startSpeakUseCase.execute(param).launchCollect(viewModelScope) { state ->

            speakState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopSpeak() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopSpeakUseCase.execute()
    }

    fun getTextFromImage(path: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        detectState.postValue(ResultState.Running(path))

        val state = detectUseCase.execute(DetectUseCase.Param(path, "en", "en", DetectOption.TEXT, 500))

        state.doSuccess { list ->

            detectState.postValue(ResultState.Success(list.joinToString("\n") { it.text }))
        }

        state.doFailed {

            detectState.postValue(ResultState.Failed(it))
        }
    }

    data class SpeakInfo(
        val isShowPlay: Boolean = false,
        val isShowPause: Boolean = false,
    )

    data class ImageInfo(
        val image: String,
        val isShowInput: Boolean = false,
        val isShowImage: Boolean = false,
    )

    data class ClearInfo(
        val text: String = "",
        val isShow: Boolean = false,
    )

    data class ReverseInfo(
        val text: String = "",
        val isShow: Boolean = false,
        val isSelected: Boolean = false,
    )
}