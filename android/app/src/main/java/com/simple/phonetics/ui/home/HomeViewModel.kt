package com.simple.phonetics.ui.home

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.detect.data.usecase.DetectUseCase
import com.simple.detect.entities.DetectOption
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.R
import com.simple.phonetics.TYPE_HISTORY
import com.simple.phonetics.TYPE_VERSION
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toViewItem
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val detectUseCase: DetectUseCase,
    private val stopReadingUseCase: StopReadingUseCase,
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase
) : BaseViewModel() {

    val title: LiveData<CharSequence> = combineSources(theme, translate) {

        val theme = theme.get()
        val translate = translate.getOrEmpty()

        val title = translate["Ephonetics"].orEmpty()
            .with("Ep", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
            .with("honetics", ForegroundColorSpan(theme.colorOnSurface))

        postDifferentValue(title)
    }


    @VisibleForTesting
    val text: MediatorLiveData<Pair<String, String>> = MediatorLiveData("" to "")


    @VisibleForTesting
    val isSupportTranslate: LiveData<Boolean> = MediatorLiveData()


    @VisibleForTesting
    val detectState: LiveData<ResultState<String>> = MediatorLiveData()
    val detectStateEvent: LiveData<Event<ResultState<String>>> = detectState.toEvent()

    val imageInfo: LiveData<ImageInfo> = listenerSources(detectState) {

        val detectState = detectState.value

        val info = ImageInfo(
            image = detectState?.toRunning()?.data.orEmpty(),
            isShow = !detectState.isCompleted(),
        )

        postDifferentValue(info)
    }


    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val isSupportReverse: LiveData<Boolean> = combineSources(inputLanguage, outputLanguage, isSupportTranslate) {

        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()
        val isSupportTranslate = isSupportTranslate.get()

        postDifferentValue(inputLanguage.id != outputLanguage.id && isSupportTranslate)
    }

    val reverseInfo: LiveData<ReverseInfo> = combineSources(theme, translate, isReverse, isSupportReverse) {

        val theme = theme.get()
        val translate = translate.get()

        val isReverse = isReverse.get()
        val isSupportReverse = isSupportReverse.get()

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
                .with(ForegroundColorSpan(textColor)),
            isShow = isSupportReverse,
            background = Background(
                strokeWidth = DP.DP_1 + DP.DP_05.toInt(),
                cornerRadius = DP.DP_8,
                strokeColor = theme.colorPrimary,
                backgroundColor = backgroundColor
            )
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val readingState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Success(""))

    val readingInfo: LiveData<ReadingInfo> = listenerSources(text, readingState, isSupportReading) {

        val text = text.value ?: return@listenerSources
        val listenState = readingState.value
        val isSupportReading = isSupportReading.value ?: return@listenerSources && text.second.isNotBlank()

        val info = ReadingInfo(
            isShowPlay = !listenState.isRunning() && isSupportReading,
            isShowPause = listenState.isRunning() && isSupportReading
        )

        postDifferentValue(info)
    }


    val clearInfo: LiveData<ClearInfo> = combineSources(theme, translate, text) {

        val text = text.get()
        val theme = theme.get()
        val translate = translate.get()

        val info = ClearInfo(
            text = translate["action_clear"].orEmpty()
                .with(ForegroundColorSpan(theme.colorPrimary)),
            isShow = text.second.isNotBlank(),
            background = Background(
                strokeWidth = DP.DP_1 + DP.DP_05.toInt(),
                cornerRadius = DP.DP_8,
                strokeColor = theme.colorPrimary,
                backgroundColor = Color.TRANSPARENT
            ),
        )

        postDifferentValue(info)
    }

    val enterInfo: LiveData<EnterInfo> = combineSources(theme, translate, isReverse, outputLanguage, inputLanguage) {

        val theme = theme.get()
        val translate = translate.get()
        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageName = if (isReverse.value == true) {
            outputLanguage.name
        } else {
            inputLanguage.name
        }

        val hint = translate["hint_enter_language_text"].orEmpty()
            .replace("\$language_name", languageName)

        val info = EnterInfo(
            hint = hint
                .with(ForegroundColorSpan(theme.colorOnSurfaceVariant))
                .with(languageName, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
            textColor = theme.colorOnSurface,
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(text, isReverse, inputLanguage, outputLanguage, phoneticCodeSelected) {

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
    val phoneticsViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(size, theme, translate, phoneticsState, phoneticCodeSelected, isSupportSpeak, isSupportReading, isSupportTranslate) {

        val theme = theme.get()
        val translate = translate.get()

        val state = phoneticsState.get()
        val phoneticsCode = phoneticCodeSelected.get()
        val isSupportTranslate = isSupportTranslate.get()

        state.doStart {

            postDifferentValue(getPhoneticLoadingViewItem(theme))
            return@combineSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        val listItem = state.toSuccess()?.data.orEmpty()

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,
                isSupportTranslate = isSupportTranslate,

                theme = theme,
                translate = translate,

                isSupportSpeak = isSupportSpeak.value == true,
                isSupportListen = isSupportReading.value == true
            )
        }.let {

            viewItemList.addAll(it)
        }

        if (viewItemList.isNotEmpty()) TitleViewItem(
            id = "TITLE_RESULT",
            text = translate["title_result"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
        ).let {

            viewItemList.add(0, it)
            viewItemList.add(0, SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_8))
        }

        postDifferentValueIfActive(viewItemList)
    }.apply {

        postDifferentValue(emptyList())
    }


    @VisibleForTesting
    val typeViewItemList: LiveData<HashMap<Int, List<ViewItem>>> = combineSources(theme, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val map = hashMapOf<Int, List<ViewItem>>()

        map[TYPE_VERSION] = versionViewItem(theme = theme, translate = translate)

        postDifferentValue(map)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(size, translate, typeViewItemList, phoneticsViewItemList) {

        val size = size.get()
        val translate = translate.get()

        val typeViewItemList = typeViewItemList.get().toMutableMap()
        val phoneticsViewItemList = phoneticsViewItemList.getOrEmpty()

        val list = arrayListOf<ViewItem>()

        list.addAll(phoneticsViewItemList)

        /**
         * nếu không có dữ liệu phonetic và không có dữ liệu history thì bỏ qua
         */
        if (list.isEmpty() && !typeViewItemList.containsKey(TYPE_HISTORY)) {

            return@combineSources
        }


        val versionViewItemList = typeViewItemList.remove(TYPE_VERSION)

        if (list.isEmpty()) typeViewItemList.toList().sortedBy {

            it.first
        }.forEach {

            list.addAll(it.second)
        }

        if (list.isEmpty() || typeViewItemList[TYPE_HISTORY].isNullOrEmpty()) com.simple.coreapp.ui.adapters.EmptyViewItem(
            id = "EMPTY",
            message = translate["message_result_empty"].orEmpty(),
            imageRes = R.raw.anim_empty
        ).let {

            list.add(it)
        }

        /**
         * dữ liệu version đặt ở cuối cùng
         */
        versionViewItemList?.let {

            list.addAll(it)
        }

        list.add(SpaceViewItem(id = "BOTTOM_0", height = DP.DP_350))
        list.add(SpaceViewItem(id = "BOTTOM_1", height = DP.DP_100))

        postDifferentValueIfActive(list)
    }

    val isShowLoading: LiveData<Boolean> = listenerSources(readingState, detectState) {

        postDifferentValue(readingState.value.isStart() || detectState.value.isRunning())
    }


    init {

        isSupportSpeak.asFlow().launchIn(viewModelScope)
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
    }

    fun updateSupportTranslate(b: Boolean) {

        this.isSupportTranslate.postDifferentValue(b)
    }

    fun updateTypeViewItemList(type: Int, it: List<ViewItem>) = viewModelScope.launch(handler + Dispatchers.IO) {

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
            inputCode = inputLanguage.value?.id ?: "en",
            outputCode = inputLanguage.value?.id ?: "en",
            detectOption = DetectOption.TEXT,
            sizeMax = 500
        )

        val state = detectUseCase.execute(param)

        state.doSuccess { list ->

            detectState.postValue(ResultState.Success(list.joinToString("\n") { it.text }))
        }

        state.doFailed {

            detectState.postValue(ResultState.Failed(it))
        }
    }

    private fun versionViewItem(theme: AppTheme, translate: Map<String, String>) = arrayListOf<ViewItem>().apply {

        if (translate.containsKey("version_name")) NoneTextViewItem(
            id = "VERSION",
            text = translate["version_name"]
                .orEmpty()
                .replace("\$version", BuildConfig.VERSION_NAME)
                .with(BuildConfig.VERSION_NAME, ForegroundColorSpan(theme.colorPrimary), StyleSpan(Typeface.BOLD)),
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
        val hint: CharSequence = "",
        val textColor: Int,
    )

    data class ClearInfo(
        val text: CharSequence = "",
        val isShow: Boolean = false,

        val background: Background
    )

    data class ReverseInfo(
        val text: CharSequence = "",
        val isShow: Boolean = false,

        val background: Background
    )
}