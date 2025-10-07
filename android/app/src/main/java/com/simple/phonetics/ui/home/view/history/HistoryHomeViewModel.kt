package com.simple.phonetics.ui.home.view.history

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.home.adapters.HistoryViewItem
import com.simple.state.ResultState
import com.simple.state.toSuccess
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface

class HistoryHomeViewModel(
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val historyState: LiveData<ResultState<List<Sentence>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getPhoneticsHistoryAsyncUseCase.execute(null).collect { list ->

            postValue(ResultState.Success(list))
        }
    }

    val historyViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, style, theme, translate, historyState) {

        val size = size.value ?: return@combineSourcesWithDiff
        val style = style.value ?: return@combineSourcesWithDiff
        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val historyState = historyState.value ?: return@combineSourcesWithDiff

        if (historyState !is ResultState.Success) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        val historyList = historyState.toSuccess()?.data.orEmpty()

        if (historyList.isNotEmpty()) TextSimpleViewItem(
            id = "TITLE_HISTORY",
            text = translate["title_history"].orEmpty()
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
            textStyle = R.style.TextAppearance_MaterialComponents_Headline6,
            margin = Margin(
                marginHorizontal = DP.DP_4
            )
        ).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_HISTORY_0", width = size.width, height = DP.DP_16))
            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_HISTORY_1", width = size.width, height = DP.DP_8))
        }

        historyList.mapIndexed { _, sentence ->

            HistoryViewItem(
                id = sentence.text,
                text = sentence.text.with(ForegroundColor(theme.colorOnSurface)),
            )
        }.let {

            viewItemList.addAll(it)
        }


        viewItemList.forEach {

            if (it is SizeViewItem) it.measure(appSize = size, style = style)
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("history_home_show")
        }

        postValueIfActive(viewItemList)
    }
}