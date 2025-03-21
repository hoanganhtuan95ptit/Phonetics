package com.simple.phonetics.ui.game.items.ipa_puzzle

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.Id
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.AppSize
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.exts.TitleViewItem

fun getIPAPuzzleStateInfo(size: AppSize, theme: AppTheme, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, isAnswerCorrect: Boolean): GameItemViewModel.StateInfo {

    val textColor = if (isAnswerCorrect) {
        theme.colorOnPrimaryVariant
    } else {
        theme.colorOnErrorVariant
    }

    val title = if (isAnswerCorrect) {
        translate["title_answer_true"].orEmpty()
    } else {
        translate["title_answer_failed"].orEmpty()
    }.with(ForegroundColorSpan(textColor))


    val param1 = "/${quiz.question.ipaMissing}/"

    val message = translate["game_ipa_puzzle_screen_message_answer"].orEmpty()
        .replace("\$param1", param1)
        .with(ForegroundColorSpan(textColor))
        .with(param1, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
        .trim()

    val buttonText = if (isAnswerCorrect) {
        translate["action_continue"].orEmpty()
    } else {
        translate["action_retry"].orEmpty()
    }.with(ForegroundColorSpan(theme.colorOnPrimary))

    val positive = ButtonInfo(
        text = buttonText,
        background = Background(
            strokeWidth = 0,
            cornerRadius = DP.DP_16,
            backgroundColor = if (isAnswerCorrect) {
                theme.colorPrimary
            } else {
                theme.colorError
            }
        )
    )

    val info = GameItemViewModel.StateInfo(
        title = title,
        message = message,
        positive = positive,

        background = Background(
            cornerRadius_TR = DP.DP_16,
            cornerRadius_TL = DP.DP_16,
            backgroundColor = if (isAnswerCorrect) {
                theme.colorPrimaryVariant
            } else {
                theme.colorErrorVariant
            }
        )
    )

    return info
}

fun getIPAPuzzleTitleViewItem(size: AppSize, theme: AppTheme, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz): ViewItem {

    return TitleViewItem(
        id = "TITLE",
        text = translate["game_ipa_puzzle_screen_title"].orEmpty()
            .replace("\$param1", quiz.question.text)
            .replace("\$param2", quiz.question.ipaIncomplete)
            .with(ForegroundColorSpan(theme.colorOnSurface))
            .with(quiz.question.text, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface))
            .with(quiz.question.ipaIncomplete, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface))
            .with("____", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorError)),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    )
}

fun getIPAPuzzleOptionViewItem(size: AppSize, theme: AppTheme, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, choose: String?) = quiz.answers.mapIndexed { index, answer ->

    ClickTextViewItem(
        id = "${Id.CHOOSE}_$index",
        data = answer,

        text = answer
            .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
        textStyle = TextStyle(
            textSize = 16f,
            textGravity = Gravity.CENTER
        ),
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.MATCH_PARENT
        ),
        textBackground = Background(
            cornerRadius = DP.DP_16,
            strokeWidth = DP.DP_2,
            strokeColor = if (answer.equals(choose, true)) {
                theme.colorPrimary
            } else {
                theme.colorOnSurfaceVariant
            }
        ),

        size = Size(
            width = (size.width - 2 * DP.DP_8) / 2,
            height = (size.width - 2 * DP.DP_8) / 2 + DP.DP_16
        ),
        padding = Padding(
            paddingVertical = DP.DP_8,
            paddingHorizontal = DP.DP_8
        )
    )
}

fun getIPAPuzzleLoadingViewItem(size: AppSize, theme: AppTheme): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val background = Background(
        cornerRadius = DP.DP_24,
        backgroundColor = theme.colorLoading
    )

    NoneTextViewItem(
        id = "LOADING_1",
        textSize = Size(
            width = DP.DP_350,
            height = DP.DP_18
        ),
        textMargin = Margin(
            marginVertical = DP.DP_8,
            marginHorizontal = DP.DP_8,
        ),
        textBackground = background
    ).let {

        add(it)
    }

    for (i in 0..3) NoneTextViewItem(
        id = "LOADING_ITEM_$i",
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.MATCH_PARENT
        ),
        textMargin = Margin(
            marginVertical = DP.DP_8,
            marginHorizontal = DP.DP_8,
        ),
        textBackground = background,

        size = Size(
            width = (size.width - 2 * DP.DP_8) / 2,
            height = (size.width - 2 * DP.DP_8) / 2 + DP.DP_16
        ),
    ).let {

        add(it)
    }
}