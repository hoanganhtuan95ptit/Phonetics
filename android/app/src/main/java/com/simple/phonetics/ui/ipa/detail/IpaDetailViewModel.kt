package com.simple.phonetics.ui.ipa.detail

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.dao.entities.Ipa
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailViewItem
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorLoading
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.phonetics.utils.exts.toViewItem
import com.simple.phonetics.utils.provider.ipa_reading.IpaReading
import com.simple.phonetics.utils.spans.RoundedBackground
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doStart
import com.simple.state.doSuccess
import com.simple.state.isRunning
import com.simple.state.isStart
import com.simple.state.toSuccess
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class IpaDetailViewModel(
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsAsyncUseCase: GetPhoneticsAsyncUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : BaseViewModel() {

    private var readingIpaJob: Job? = null
    private var readingTextJob: Job? = null

    val ipa: LiveData<Ipa> = MediatorLiveData()

    val title: LiveData<String> = combineSourcesWithDiff(ipa, translate) {

        val ipa = ipa.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        postValueIfActive(translate["ipa_detail_screen_" + ipa.type.lowercase()])
    }


    val readingState: LiveData<ResultState<String>> = MediatorLiveData()

    @VisibleForTesting
    val ipaViewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(theme, ipa, readingState) {

        val theme = theme.value ?: return@listenerSourcesWithDiff

        val ipa = ipa.value ?: return@listenerSourcesWithDiff
        val readingState = readingState.value

        val viewItemList = arrayListOf<ViewItem>()

        IpaDetailViewItem(
            id = ipa.ipa,

            data = ipa,

            ipa = ipa.ipa.with(ForegroundColor(theme.colorOnSurface)),

            image = if (readingState.isRunning()) {
                R.drawable.ic_pause_black_24dp
            } else {
                R.drawable.ic_play_black_24dp
            },
            isShowLoading = readingState.isStart(),

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = ipa.BackgroundColor(theme)
            )
        ).let {

            viewItemList.add(it)
        }

        postValue(viewItemList)
    }


    @VisibleForTesting
    val phoneticsState: LiveData<ResultState<List<Any>>> = combineSources(ipa, inputLanguage, outputLanguage, phoneticCodeSelected) {

        postValue(ResultState.Start)

        val param = GetPhoneticsAsyncUseCase.Param(
            textNew = ipa.get().examples.joinToString(separator = " ") { it },

            isReverse = false,
            saveToHistory = false,
            phoneticCode = phoneticCodeSelected.get(),
            inputLanguageCode = inputLanguage.get().id,
            outputLanguageCode = outputLanguage.get().id
        )

        getPhoneticsAsyncUseCase.execute(param).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticsViewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(theme, translate, phoneticsState, phoneticCodeSelected, isSupportSpeak, isSupportReading) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val state = phoneticsState.value ?: return@listenerSourcesWithDiff
        val phoneticsCode = phoneticCodeSelected.value ?: return@listenerSourcesWithDiff

        state.doStart {

            postValue(getLoadingViewItem(theme))
            return@listenerSourcesWithDiff
        }

        val viewItemList = arrayListOf<ViewItem>()


        TitleViewItem(
            id = "TITLE_EXAMPLE",
            text = translate["ipa_detail_screen_title_example"].orEmpty()
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
        ).let {

            viewItemList.add(it)
        }

        val listItem = state.toSuccess()?.data.orEmpty()

        listItem.flatMapIndexed { indexItem: Int, item: Any ->

            item.toViewItem(
                index = indexItem,
                total = listItem.lastIndex,

                phoneticsCode = phoneticsCode,

                isShowSpeak = false,

                isSupportSpeak = isSupportSpeak.value == true,
                isSupportListen = isSupportReading.value == true,
                isSupportTranslate = false,

                theme = theme,
                translate = translate
            )
        }.let {

            viewItemList.addAll(it)
        }

        postValueIfActive(viewItemList)
    }

    @VisibleForTesting
    val gameViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, translate, ipa, phoneticCodeSelected) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val ipa = ipa.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        if (!translate.containsKey("ipa_detail_screen_practice_with_games")) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val param = GetPhoneticsRandomUseCase.Param(
            resource = ipa.ipa,
            phoneticsCode = phoneticCodeSelected,

            limit = 4,
            textLengthMin = 2,
            textLengthMax = 20
        )

        val phoneticList = getPhoneticsRandomUseCase.execute(param = param)

        if (phoneticList.isEmpty()) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val list = arrayListOf<ViewItem>()

        ClickTextViewItem(
            id = Id.GAME,
            data = ipa.ipa,
            text = translate["ipa_detail_screen_practice_with_games"]
                .orEmpty()
                .replace("\$ipa", ipa.ipa)
                .with(Bold, ForegroundColor(theme.colorPrimary))
                .with(ipa.ipa, Bold, RoundedBackground(backgroundColor = theme.colorErrorVariant, textColor = theme.colorOnErrorVariant)),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT
            ),
            textStyle = TextStyle(
                textGravity = Gravity.CENTER
            ),
            textPadding = Padding(
                left = DP.DP_16,
                right = DP.DP_16
            ),
            textBackground = Background(
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_76
            ),
            margin = Margin(
                marginHorizontal = DP.DP_40
            ),
            background = DEFAULT_BACKGROUND,

            imageLeft = null,
            imageRight = null
        ).let {

            list.add(it)
        }

        if (list.isNotEmpty()) {
            logAnalytics("game_ipa_show")
        }

        postValue(list)
    }


    val viewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(ipaViewItemList, gameViewItemList, phoneticsViewItemList) {

        val list = arrayListOf<ViewItem>()

        ipaViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_IPA", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_40))
        }

        phoneticsViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_PHONETICS", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        gameViewItemList.getOrEmpty().let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_GAME", width = ViewGroup.LayoutParams.MATCH_PARENT, height = DP.DP_24))
        }

        postValueIfActive(list)
    }

    fun updateIpa(ipa: Ipa) {

        this.ipa.postDifferentValue(ipa)
    }

    fun startReading(data: Ipa) = viewModelScope.launch(handler + Dispatchers.IO) {

        readingIpaJob?.cancel()

        readingIpaJob = IpaReading.install.first().minByOrNull { it.order() }!!.reading(ipa = data, phoneticCode = phoneticCodeSelected.value.orEmpty()).launchCollect(viewModelScope) {

            readingState.postValue(it)
        }
    }

    fun startReading(text: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        readingState.postValue(ResultState.Start)

        val param = StartReadingUseCase.Param(
            text = text
        )

        readingTextJob?.cancel()

        readingTextJob = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

            readingState.postValue(state)

            state.doSuccess {
                readingTextJob?.cancel()
            }

            state.doFailed {
                readingTextJob?.cancel()
            }
        }
    }

    private fun getLoadingViewItem(theme: Map<String, Any>): List<ViewItem> = arrayListOf<ViewItem>().apply {

        val background = Background(
            cornerRadius = DP.DP_24,
            backgroundColor = theme.colorLoading
        )

        add(IpaDetailLoadingViewItem(id = "1", background = background))

        add(SpaceViewItem(id = "2", height = DP.DP_24))

        addAll(getPhoneticLoadingViewItem(theme = theme))
    }
}