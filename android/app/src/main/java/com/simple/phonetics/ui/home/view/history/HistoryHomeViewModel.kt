package com.simple.phonetics.ui.home.view.history

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.home.adapters.HistoryViewItem
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.toSuccess

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

    val historyViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, historyState) {

        val theme = theme.get()
        val translate = translate.get()

        val historyState = historyState.get()

        if (historyState !is ResultState.Success) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        val historyList = historyState.toSuccess()?.data.orEmpty()

        if (historyList.isNotEmpty()) TitleViewItem(
            id = "TITLE_HISTORY",
            text = translate["title_history"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        ).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_HISTORY_0", height = DP.DP_16))
            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_HISTORY_1", height = DP.DP_8))
        }

        historyList.mapIndexed { _, sentence ->

            HistoryViewItem(
                id = sentence.text,
                text = sentence.text.with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            )
        }.let {

            viewItemList.addAll(it)
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("history_home_show")
        }

        postValueIfActive(viewItemList)
    }
}