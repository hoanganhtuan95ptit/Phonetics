package com.simple.phonetics.ui.home

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.toRich
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.R
import com.simple.phonetics.TYPE_HISTORY
import com.simple.phonetics.TYPE_VERSION
import com.simple.phonetics.domain.usecase.detect.DetectUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.listenerSourcesWithDiff
import com.simple.phonetics.utils.exts.mutableSharedFlow
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.phonetics.utils.exts.value
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.toRunning
import com.simple.state.toSuccess
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.height
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val detectUseCase: DetectUseCase,
    private val stopReadingUseCase: StopReadingUseCase,
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val jobQueue = JobQueue()

    val title: LiveData<RichText> = combineSourcesWithDiff(theme, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val title = translate.getOrKey("Ephonetics")
            .with(ForegroundColor(theme.colorOnSurface))
            .with("Ep", Bold, ForegroundColor(theme.colorPrimary))

        postValue(title)
    }


    @VisibleForTesting
    val text: MediatorLiveData<Pair<String, String>> = MediatorLiveData("" to "")


    @VisibleForTesting
    val isSupportTranslate: LiveData<Boolean> = MediatorLiveData(false)


    @VisibleForTesting
    val detectState: LiveData<ResultState<String>> = MediatorLiveData()
    val detectStateEvent: LiveData<Event<ResultState<String>>> = detectState.toEvent()

    val imageInfo: LiveData<ImageInfo> = listenerSourcesWithDiff(detectState) {

        val detectState = detectState.value

        val info = ImageInfo(
            image = detectState?.toRunning()?.data.orEmpty(),
            isShow = !detectState.isCompleted(),
        )

        postValue(info)
    }


    @Deprecated("")
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    val isReverseFlow = mutableSharedFlow {

        emit(false)
    }


    @VisibleForTesting
    val isSupportReverse: LiveData<Boolean> = combineSourcesWithDiff(inputLanguage, outputLanguage, isSupportTranslate) {

        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()
        val isSupportTranslate = isSupportTranslate.get()

        postValue(inputLanguage.id != outputLanguage.id && isSupportTranslate)
    }

    val reverseInfo: LiveData<ReverseInfo> = listenerSourcesWithDiff(theme, translate, isReverse, isSupportReverse) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val isReverse = isReverse.value ?: return@listenerSourcesWithDiff
        val isSupportReverse = isSupportReverse.value ?: false

        val textColor = if (isReverse)
            theme.colorOnPrimaryVariant
        else
            theme.colorPrimary

        val backgroundColor = if (isReverse)
            theme.colorPrimaryVariant
        else
            Color.TRANSPARENT

        val info = ReverseInfo(
            text = translate["action_reverse"].orEmpty()
                .with(ForegroundColor(textColor)),
            isShow = isSupportReverse,
            background = Background(
                strokeWidth = DP.DP_1 + DP.DP_05.toInt(),
                cornerRadius = DP.DP_8,
                strokeColor = theme.colorPrimary,
                backgroundColor = backgroundColor
            )
        )

        postValue(info)
    }


    @VisibleForTesting
    val readingState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Success(""))

    val readingInfo: LiveData<ReadingInfo> = listenerSourcesWithDiff(text, readingState, isSupportReading) {

        val text = text.value ?: return@listenerSourcesWithDiff
        val listenState = readingState.value
        val isSupportReading = isSupportReading.value ?: return@listenerSourcesWithDiff && text.second.isNotBlank()

        val info = ReadingInfo(
            isShowPlay = !listenState.isRunning() && isSupportReading,
            isShowPause = listenState.isRunning() && isSupportReading
        )

        postValue(info)
    }


    val clearInfo: LiveData<ClearInfo> = combineSourcesWithDiff(theme, translate, text) {

        val theme = theme.get()
        val translate = translate.get()

        val text = text.get()

        val info = ClearInfo(
            text = translate["action_clear"].orEmpty()
                .with(ForegroundColor(theme.colorPrimary)),
            isShow = text.second.isNotBlank(),
            background = Background(
                strokeWidth = DP.DP_1 + DP.DP_05.toInt(),
                cornerRadius = DP.DP_8,
                strokeColor = theme.colorPrimary,
                backgroundColor = Color.TRANSPARENT
            ),
        )

        postValue(info)
    }

    val enterInfo: Flow<EnterInfo> = listenerSourcesWithDiff(themeFlow, translateFlow, isReverseFlow, outputLanguageFlow, inputLanguageFlow) {

        val theme = themeFlow.value ?: return@listenerSourcesWithDiff
        val translate = translateFlow.value ?: return@listenerSourcesWithDiff
        val inputLanguage = inputLanguageFlow.value ?: return@listenerSourcesWithDiff
        val outputLanguage = outputLanguageFlow.value ?: return@listenerSourcesWithDiff

        val languageName = if (isReverseFlow.value == true) {
            outputLanguage.name
        } else {
            inputLanguage.name
        }

        val hint = translate["hint_enter_language_text"].orEmpty()
            .replace("\$language_name", languageName)

        val info = EnterInfo(
            hint = hint
                .with(ForegroundColor(theme.colorOnSurfaceVariant))
                .with(languageName, Bold, ForegroundColor(theme.colorOnSurface)),
            textColor = theme.colorOnSurface,
        )

        emit(info)
    }


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Sentence>>> = combineSources(text, isReverse, inputLanguage, outputLanguage, phoneticCodeSelected) {

        val text = text.get()

        val param = GetPhoneticsAsyncUseCase.Param(
            textOld = text.first,
            textNew = text.second,

            isReverse = isReverse.get(),
            phoneticCode = phoneticCodeSelected.get(),
            inputLanguageCode = inputLanguage.get().id,
            outputLanguageCode = outputLanguage.get().id
        )

        getPhoneticsAsyncUseCase.execute(param).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticsViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff<List<ViewItem>>(size, theme, translate, phoneticsState, phoneticCodeSelected, isSupportSpeak, isSupportReading, isSupportTranslate) {

        val theme = theme.get()
        val translate = translate.get()

        val state = phoneticsState.get()
        val isSupportTranslate = isSupportTranslate.get()

        state.doStart {

            postValue(getPhoneticLoadingViewItem(theme))
            return@combineSourcesWithDiff
        }

        val viewItemList = arrayListOf<ViewItem>()

        state.toSuccess()?.data.orEmpty().toViewItem(
            isSupportSpeak = isSupportSpeak.value == true,
            isSupportListen = isSupportReading.value == true,
            isSupportTranslate = isSupportTranslate,

            theme = theme,
            translate = translate
        ).let {

            viewItemList.addAll(it)
        }

        if (viewItemList.isNotEmpty()) TitleViewItem(
            id = "TITLE_RESULT",
            text = translate["title_result"].orEmpty()
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
        ).let {

            viewItemList.add(0, it)
            viewItemList.add(0, SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_8))
        }

        postValueIfActive(viewItemList)
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val typeViewItemList: LiveData<HashMap<Int, List<ViewItem>>> = combineSourcesWithDiff(theme, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val map = value ?: hashMapOf()

        map[TYPE_VERSION] = versionViewItem(theme = theme, translate = translate)

        postValue(map)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, style, translate, typeViewItemList, phoneticsViewItemList) {

        val size = size.get()
        val style = style.get()
        val translate = translate.get()

        val typeViewItemList = typeViewItemList.get().toMutableMap()
        val phoneticsViewItemList = phoneticsViewItemList.getOrEmpty()

        val list = arrayListOf<ViewItem>()

        list.addAll(phoneticsViewItemList)

        /**
         * nếu không có dữ liệu phonetic và không có dữ liệu history thì bỏ qua
         */
        if (list.isEmpty() && !typeViewItemList.containsKey(TYPE_HISTORY)) {

            return@combineSourcesWithDiff
        }


        val versionViewItemList = typeViewItemList.remove(TYPE_VERSION)

        if (list.isEmpty()) typeViewItemList.toList().sortedBy {

            it.first
        }.forEach {

            list.addAll(it.second)
        }

        if (list.isEmpty() || typeViewItemList[TYPE_HISTORY].isNullOrEmpty()) com.simple.coreapp.ui.adapters.EmptyViewItem(
            id = "EMPTY",
            message = translate["message_result_empty"].orEmpty().toRich(),
            imageRes = R.raw.anim_empty
        ).let {

            list.add(it)
        }

        /**
         * dữ liệu version đặt ở cuối cùng
         */
        if (phoneticsViewItemList.isEmpty()) versionViewItemList?.let {

            list.addAll(it)
        }

        list.add(SpaceViewItem(id = "BOTTOM_0", height = size.height - DP.DP_350))

        postValueIfActive(list)
    }

    val isShowLoading: LiveData<Boolean> = listenerSourcesWithDiff(readingState, detectState) {

        postValue(readingState.value.isStart() || detectState.value.isRunning())
    }


    init {

        isSupportSpeak.asFlow().launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        jobQueue.cancel()
    }


    fun getPhonetics(text: String) {

        if (text.isBlank()) {

            this.text.value = "" to text
        } else {

            this.text.postDifferentValue(this.text.value?.second.orEmpty() to text)
        }
    }

    fun switchReverse() {

        this.isReverse.postValue(!this.isReverse.get())
        this.isReverseFlow.tryEmit(!this.isReverse.get())
    }

    fun updateSupportTranslate(b: Boolean) {

        this.isSupportTranslate.postValue(b)
    }

    fun updateTypeViewItemList(type: Int, it: List<ViewItem>) = jobQueue.submit(handler + Dispatchers.IO) {

        val map = typeViewItemList.asFlow().first()

        map[type] = it

        typeViewItemList.postValue(map)
    }

    fun startReading(text: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        readingState.postValue(ResultState.Start)

        val param = StartReadingUseCase.Param(
            text = text
        )

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

    fun getTextFromImage(path: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        detectState.postValue(ResultState.Running(path))

        val param = DetectUseCase.Param(
            path = path,
            inputLanguageCode = inputLanguage.value?.id ?: Language.EN,
            outputLanguageCode = inputLanguage.value?.id ?: Language.EN,
        )

        val state = detectUseCase.execute(param)

        detectState.postValue(state)
    }

    private fun versionViewItem(theme: Map<String, Any>, translate: Map<String, String>) = arrayListOf<ViewItem>().apply {

        if (translate.containsKey("version_name")) NoneTextViewItem(
            id = "VERSION",
            text = translate["version_name"]
                .orEmpty()
                .replace("\$version", BuildConfig.VERSION_NAME)
                .with(ForegroundColor(theme.colorOnSurface))
                .with(BuildConfig.VERSION_NAME, ForegroundColor(theme.colorPrimary), Bold),
            textSize = Size(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            textStyle = TextStyle(
                textSize = 14f,
                textGravity = Gravity.CENTER
            ),

            size = Size(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            margin = Margin(top = DP.DP_16)
        ).let {

            add(it)
        }
    }

    data class ReadingInfo(
        val isShowPlay: Boolean = false,
        val isShowPause: Boolean = false,
    )

    data class ImageInfo(
        val image: String,
        val isShow: Boolean = false,
    )

    data class EnterInfo(
        val hint: RichText = emptyText(),
        val textColor: Int,
    )

    data class ClearInfo(
        val text: RichText = emptyText(),
        val isShow: Boolean = false,

        val background: Background
    )

    data class ReverseInfo(
        val text: RichText = emptyText(),
        val isShow: Boolean = false,

        val background: Background
    )
}