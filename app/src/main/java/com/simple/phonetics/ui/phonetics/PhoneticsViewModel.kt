package com.simple.phonetics.ui.phonetics

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.LoadingViewItem
import com.simple.adapter.ViewItemCloneable
import com.simple.core.entities.Comparable
import com.simple.core.utils.extentions.hasChar
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.base.viewmodels.BaseViewModel
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.liveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toPx
import com.simple.detect.data.usecase.DetectUseCase
import com.simple.detect.entities.DetectOption
import com.simple.phonetics.R
import com.simple.phonetics.domain.entities.Language
import com.simple.phonetics.domain.entities.Phonetics
import com.simple.phonetics.domain.entities.Sentence
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.ui.adapters.TitleViewItem
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsHistoryViewItem
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsViewItem
import com.simple.phonetics.ui.phonetics.adapters.SentenceViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.state.isSuccess
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhoneticsViewModel(
    private val detectUseCase: DetectUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase,
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    private var timePost = 0L

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
    val translateState: LiveData<ResultState<Boolean>> = MediatorLiveData()


    @VisibleForTesting
    val historyViewItemList: LiveData<List<PhoneticsHistoryViewItem>> = liveData {

        getPhoneticsHistoryAsyncUseCase.execute(null).collect { list ->

            list.mapIndexed { index, sentence ->

                PhoneticsHistoryViewItem(sentence.text).refresh(index == list.lastIndex)
            }.let {

                postDifferentValue(it)
            }
        }
    }

    @VisibleForTesting
    val detectState: LiveData<ResultState<String>> = MediatorLiveData<ResultState<String>>().apply {

        postValue(ResultState.Success(""))
    }


    val text: LiveData<String> = combineSources(detectState) {

        detectState.get().doSuccess {

            postDifferentValue(it)
        }
    }


    val isReverse: LiveData<Boolean> = MediatorLiveData(true)

    val isSupportReverse: LiveData<Boolean> = combineSources(translateState) {

        postValue(translateState.get().isSuccess())
    }


    @VisibleForTesting
    val speakState: LiveData<Boolean> = MediatorLiveData()

    @VisibleForTesting
    val isSpeakStatus: LiveData<Boolean> = MediatorLiveData(false)


    @VisibleForTesting
    val phoneticsCode: LiveData<String> = MediatorLiveData()


    @VisibleForTesting
    val inputLanguage: LiveData<Language> = MediatorLiveData()

    val outputLanguage: LiveData<Language> = MediatorLiveData()

    @VisibleForTesting
    val isSupportTranslate: LiveData<Boolean> = MediatorLiveData()


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(text, isReverse, inputLanguage, outputLanguage) {

        val inputLanguageCode = inputLanguage.get().code

        val outputLanguageCode = outputLanguage.get().code

        getPhoneticsAsyncUseCase.execute(GetPhoneticsAsyncUseCase.Param(text.get(), isReverse.get(), inputLanguageCode, outputLanguageCode)).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticsViewItemList: LiveData<List<ViewItemCloneable>> = combineSources<List<ViewItemCloneable>>(phoneticsCode, isSupportTranslate, phoneticsState) {

        val phoneticsCode = phoneticsCode.get()

        val isSupportTranslate = isSupportTranslate.get()


        val state = phoneticsState.get()


        state.doStart {

            postDifferentValue(itemLoading)
            return@combineSources
        }


        val listItem = state.toSuccess()?.data

        listItem?.flatMapIndexed { indexItem: Int, item: Any ->

            if (item is Phonetics) {

                return@flatMapIndexed listOf(PhoneticsViewItem("${indexItem * 1000}", item).refresh(phoneticsCode))
            }


            if (item !is Sentence) {

                return@flatMapIndexed emptyList()
            }


            val list = arrayListOf<ViewItemCloneable>()

            item.phonetics.mapIndexed { indexPhonetic, phonetic ->

                PhoneticsViewItem("${indexItem * 1000 + indexPhonetic}", phonetic).refresh(phoneticsCode)
            }.let {

                list.addAll(it)
            }

            if (item.text.hasChar() && isSupportTranslate) SentenceViewItem("${indexItem * 1000}", item).refresh(indexItem == listItem.lastIndex).let {

                list.add(it)
            }

            list
        }?.let {

            val list = it.toMutableList()

            list.add(SpaceViewItem(height = 100.toPx()))

            postDifferentValue(list)
        }
    }.apply {

        postDifferentValue(emptyList())
    }

    @VisibleForTesting
    val listViewItem: LiveData<List<ViewItemCloneable>> = combineSources(text, historyViewItemList, phoneticsViewItemList) {

        val text = text.get()

        val historyViewItemList = historyViewItemList.getOrEmpty()


        val viewItemList = arrayListOf<ViewItemCloneable>()


        if (historyViewItemList.isNotEmpty() && text.isBlank()) {

            viewItemList.add(TitleViewItem(R.string.title_history, paddingHorizontal = 12.toPx()).refresh())
        }

        if (text.isNotBlank()) {

            viewItemList.addAll(phoneticsViewItemList.getOrEmpty())
        } else {

            viewItemList.addAll(historyViewItemList)
        }

        if (viewItemList.isNotEmpty()) {

            viewItemList.add(SpaceViewItem(height = 60.toPx()))
        }

        postDifferentValue(viewItemList)
    }


    @Suppress("ComplexRedundantLet")
    @VisibleForTesting
    internal val dataScreen: LiveData<PhoneticsInfoScreen> = combineSources(listViewItem, speakState, isSpeakStatus, detectState) {

        val text = text.get()

        val speakState = speakState.get()

        val detectState = detectState.get()

        val isShowClearText = text.isNotBlank()

        val isShowSpeakStatus = detectState.isSuccess() && text.isNotBlank()

        val isSpeakStatus = if (detectState.isStart() || !speakState || text.isBlank()) {
            null
        } else {
            isSpeakStatus.get()
        }

        val listViewItem = listViewItem.getOrEmpty()

        PhoneticsInfoScreen(
            listViewItem = listViewItem,
            isSpeakStatus = isSpeakStatus,
            isShowClearText = isShowClearText,
            isShowSpeakStatus = isShowSpeakStatus,
            detectState = detectState
        ).let {

            postValue(it)
        }
    }

    internal val listViewItemDisplayEvent: LiveData<Event<PhoneticsInfoScreen>> = combineSources(dataScreen) {

        val event = Event(dataScreen.get())
        event.hasBeenHandled = !this.hasActiveObservers()

        delay(350 - (System.currentTimeMillis() - timePost))

        postValue(event)

        timePost = System.currentTimeMillis()
    }


    fun getPhonetics(text: String) {

        this.text.postDifferentValue(text)
    }

    fun switchReverse() {

        this.isReverse.postValue(!this.isReverse.get())
    }

    fun updateSpeakState(speakState: Boolean) {

        this.speakState.postDifferentValue(speakState)
    }

    fun updateSpeakStatus(speakStatus: Boolean) {

        this.isSpeakStatus.postDifferentValue(speakStatus)
    }

    fun updatePhoneticSelect(code: String) {

        this.phoneticsCode.postDifferentValue(code)
    }

    fun updateTranslateState(state: ResultState<Boolean>) {

        this.translateState.postDifferentValue(state)
    }

    fun updateInputLanguage(language: Language) {

        this.inputLanguage.postDifferentValue(language)
    }

    fun updateOutputLanguage(language: Language) {

        this.outputLanguage.postDifferentValue(language)
    }

    fun updateTranslateStatus(isSupportTranslate: Boolean) {

        this.isSupportTranslate.postDifferentValue(isSupportTranslate)
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
}

internal data class PhoneticsInfoScreen(
    val listViewItem: List<ViewItemCloneable> = emptyList(),

    val isSpeakStatus: Boolean? = null,
    val isShowClearText: Boolean? = null,
    val isShowSpeakStatus: Boolean? = null,

    val detectState: ResultState<String>
) : Comparable {

    override fun getListCompare(): List<*> {

        val list = arrayListOf<Any?>()

        list.addAll(listViewItem.flatMap { it.getListCompare() })

        list.add(isSpeakStatus)
        list.add(isShowClearText)
        list.add(isShowSpeakStatus)

        list.add(detectState)

        return list
    }
}