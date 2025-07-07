package com.simple.phonetics.ui.home.view.game

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.Constants
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.width

class GameHomeViewModel(
    private val countWordAsyncUseCase: CountWordAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val wordPopularCount: LiveData<Int> = combineSourcesWithDiff(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countWordAsyncUseCase.execute(CountWordAsyncUseCase.Param(resource = Word.Resource.Popular, languageCode = inputLanguage.id)).collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, style, theme, translate, wordPopularCount) {

        val size = size.value ?: return@combineSourcesWithDiff
        val style = style.value ?: return@combineSourcesWithDiff
        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val wordPopularCount = wordPopularCount.get()


        if (!translate.containsKey("title_game") || wordPopularCount <= Constants.WORD_COUNT_MIN) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        TextSimpleViewItem(
            id = "TITLE_GAME",
            text = translate.getOrEmpty("title_game")
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            textStyle = R.style.TextAppearance_MaterialComponents_Headline6,
        ).measure(size, style).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_0", width = size.width, height = DP.DP_16))
            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_1", width = size.width, height = DP.DP_8))
        }

        TextSimpleViewItem(
            id = Id.GAME,
            text = translate.getOrEmpty("action_play_game")
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorPrimary"))),
            textStyle = R.style.TextAppearance_MaterialComponents_Body1,

            padding = Padding(
                paddingVertical = DP.DP_20,
                paddingHorizontal = DP.DP_16
            ),
            background = Background(
                strokeColor = theme.getOrTransparent("colorPrimary"),
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),
        ).measure(size, style).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_2", width = size.width, height = DP.DP_16))
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("game_home_show")
        }

        postValueIfActive(viewItemList)
    }
}