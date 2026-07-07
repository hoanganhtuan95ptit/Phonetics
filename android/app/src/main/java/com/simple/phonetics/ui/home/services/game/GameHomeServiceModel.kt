package com.simple.phonetics.ui.home.services.game

import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.Constants
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.phonetics.utils.exts.withStyleBodyLarge
import com.simple.phonetics.utils.exts.withStyleTitleLarge
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.with
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class GameHomeServiceModel(
    private val countWordAsyncUseCase: CountWordAsyncUseCase
) : BaseViewModel() {

    val wordPopularCount: StateFlow<Int> = combineState(
        inputLanguageFlow.filterNotNull(),
        initialValue = 0
    ) { inputLanguage ->

        countWordAsyncUseCase.execute(CountWordAsyncUseCase.Param(resource = Word.Resource.Popular, languageCode = inputLanguage.id)).collect {
            value = it
        }
    }

    val viewItemList: StateFlow<List<ViewItem>> = combineState(
        sizes,
        themes,
        strings,
        wordPopularCount,
        initialValue = emptyList()
    ) { sizes, themes, strings, wordPopularCount ->

        if (!strings.containsKey("title_game") || wordPopularCount <= Constants.WORD_COUNT_MIN) {

            value = emptyList()
            return@combineState
        }


        val viewItemList = arrayListOf<ViewItem>()

        TextSimpleViewItem(
            id = "TITLE_GAME",
            maxWidth = sizes.width - 2 * 12.dp().toInt(),
            text = strings.getOrEmpty("title_game")
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

        TextSimpleViewItem(
            id = Id.GAME,
            maxWidth = sizes.width,
            text = strings.getOrEmpty("action_play_game")
                .withStyleBodyLarge()
                .with(BigBold, BigForegroundColor(themes.colorPrimary))
                .build(),

            textPadding = Padding(
                16.dp().toInt(),
            ),

            padding = Padding(
                left = 4.dp().toInt(),
                bottom = 16.dp().toInt()
            ),

            background = Background(
                strokeColor = themes.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),
        ).let {

            viewItemList.add(it)
        }

        value = viewItemList
    }

    init {

        viewItemList.map { it.isNotEmpty() }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_game_home_show_$it")
        }
    }
}