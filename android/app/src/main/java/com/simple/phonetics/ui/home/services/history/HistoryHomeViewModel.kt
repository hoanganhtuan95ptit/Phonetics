package com.simple.phonetics.ui.home.services.history

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.view.Padding
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.home.adapters.HistoryViewItem
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.withStyleBodyLarge
import com.simple.phonetics.utils.exts.withStyleBodySmall
import com.simple.phonetics.utils.exts.withStyleTitleLarge
import com.simple.state.ResultState
import com.simple.state.toSuccess
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.with
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class HistoryHomeViewModel(
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val historyState: StateFlow<ResultState<List<Sentence>>> = combineState(
        sizes,
        ResultState.Idle as ResultState<List<Sentence>>
    ) {

        getPhoneticsHistoryAsyncUseCase.execute(null).collect { list ->
            value = ResultState.Success(list)
        }
    }

    val viewItemList: StateFlow<List<ViewItem>> = combineState(
        sizes,
        themes,
        strings,
        historyState,
        initialValue = emptyList()
    ) { sizes, themes, strings, historyState ->

        if (historyState !is ResultState.Success) {
            value = emptyList()
            return@combineState
        }

        val viewItemList = arrayListOf<ViewItem>()

        val historyList = historyState.toSuccess()?.data.orEmpty()

        if (historyList.isNotEmpty()) TextSimpleViewItem(
            id = "TITLE_HISTORY",
            maxWidth = sizes.width - 2 * 12.dp().toInt(),
            text = strings["title_history"].orEmpty()
                .withStyleTitleLarge()
                .with(BigBold, BigForegroundColor(themes.colorOnSurface))
                .build(),
            textPadding = Padding(
                top = 16.dp().toInt(),
                left = 4.dp().toInt(),
                right = 4.dp().toInt(),
                bottom = 8.dp().toInt()
            )
        ).let {

            viewItemList.add(it)
        }

        historyList.mapIndexed { _, sentence ->

            HistoryViewItem(
                id = sentence.text,
                maxWidth = sizes.width - 2 * 12.dp().toInt(),
                text = sentence.text
                    .withStyleBodyLarge()
                    .with(BigForegroundColor(themes.colorOnSurface))
                    .build(),
            )
        }.let {

            viewItemList.addAll(it)
        }

        value = viewItemList
    }

    init {

        viewItemList.map { it.isNotEmpty() }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_history_home_show_$it")
        }
    }
}