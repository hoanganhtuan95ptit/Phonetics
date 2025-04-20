package com.simple.phonetics.ui.home.view.game

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.Constants
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.spans.RoundedBackgroundSpan

class GameHomeViewModel(
    private val countWordAsyncUseCase: CountWordAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val wordPopularCount: LiveData<Int> = combineSources(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countWordAsyncUseCase.execute(CountWordAsyncUseCase.Param(resource = Word.Resource.Popular, languageCode = inputLanguage.id)).collect {

            postDifferentValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, wordPopularCount) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val wordPopularCount = wordPopularCount.get()


        if (!translate.containsKey("title_game") || wordPopularCount <= Constants.WORD_COUNT_MIN) {

            postDifferentValue(emptyList())
            return@combineSources
        }


        val list = arrayListOf<ViewItem>()

        TitleViewItem(
            id = "TITLE_GAME",
            text = translate["title_game"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_0", height = DP.DP_16))
            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_1", height = DP.DP_8))
        }

        ClickTextViewItem(
            id = Id.GAME,
            text = "${translate["action_play_game"]}  BETA "
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
                .with(" BETA ", StyleSpan(Typeface.BOLD), RoundedBackgroundSpan(backgroundColor = theme.colorErrorVariant, textColor = theme.colorOnErrorVariant)),
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
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            imageLeft = null,
            imageRight = null
        ).let {

            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_TITLE_AND_GAME_2", height = DP.DP_16))
        }

        postDifferentValueIfActive(list)
    }
}