package com.simple.phonetics.ui.home.view.game

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
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
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.Constants
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent

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

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, wordPopularCount) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val wordPopularCount = wordPopularCount.get()


        if (!translate.containsKey("title_game") || wordPopularCount <= Constants.WORD_COUNT_MIN) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()

        TitleViewItem(
            id = "TITLE_GAME",
            text = translate["title_game"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        ).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_0", height = DP.DP_16))
            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_1", height = DP.DP_8))
        }

        ClickTextViewItem(
            id = Id.GAME,
            text = "${translate["action_play_game"]}"
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorPrimary"))),
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
            textBackground = DEFAULT_BACKGROUND,

            size = Size(
                height = DP.DP_76,
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            margin = Margin(
                marginVertical = DP.DP_4,
                marginHorizontal = DP.DP_4
            ),
            background = Background(
                strokeColor = theme.getOrTransparent("colorPrimary"),
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            imageLeft = null,
            imageRight = null
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_2", height = DP.DP_16))
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("game_home_show")
        }

        postValueIfActive(viewItemList)
    }
}