package com.simple.phonetics.ui.game.items.ipa_puzzle

import android.view.Gravity
import android.view.ViewGroup
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.Id
import com.simple.phonetics.ui.common.adapters.texts.ClickBigTextViewItem
import com.simple.phonetics.ui.common.adapters.texts.NoneBigTextViewItem
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.colorErrorVariant
import com.simple.phonetics.utils.exts.colorLoading
import com.simple.phonetics.utils.exts.colorOnErrorVariant
import com.simple.phonetics.utils.exts.colorOnPrimaryVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.toRich
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.with
import com.simple.ui.precompute.text.withFirst
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorError
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary

fun getIPAPuzzleStateInfo(size: Map<String, Int>, theme: Map<String, Any>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, isAnswerCorrect: Boolean): GameItemViewModel.StateInfo {

    val textColor = if (isAnswerCorrect) {
        theme.colorOnPrimaryVariant
    } else {
        theme.colorOnErrorVariant
    }

    val title = if (isAnswerCorrect) {
        translate["title_answer_true"]
    } else {
        translate["title_answer_failed"]
    }


    val param1 = "/${quiz.question.ipaMissing}/"

    val message = translate["game_ipa_puzzle_screen_message_answer"].orEmpty()
        .trim()
        .replace("\$param1", param1)
        .with(BigForegroundColor(textColor))
        .withFirst(param1, BigBold, BigForegroundColor(theme.colorPrimary)).build()


    val list = arrayListOf<ViewItem>()

    NoneBigTextViewItem(
        id = "2",
        text = title.orEmpty()
            .with(BigBold, BigForegroundColor(textColor)).build(),
        size = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
        ),
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
        ),
        textStyle = TextStyle(
            textSize = 20f,
            textGravity = Gravity.CENTER
        )
    ).let {

        list.add(it)
    }

    NoneBigTextViewItem(
        id = "3",
        text = message,
        size = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
        ),
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
        ),
        textStyle = TextStyle(
            textSize = 16f,
            textGravity = Gravity.CENTER
        )
    ).let {

        list.add(it)
    }


    val buttonText = if (isAnswerCorrect) {
        translate["action_continue"]
    } else {
        translate["action_retry"]
    }

    val positive = ButtonInfo(
        text = buttonText.orEmpty()
            .with(BigForegroundColor(theme.colorOnPrimary)).build().toRich(),
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

        viewItemList = list,

        positive = positive,

        backgroundColor = if (isAnswerCorrect) {
            theme.colorPrimaryVariant
        } else {
            theme.colorErrorVariant
        }
    )

    return info
}

fun getIPAPuzzleTitleViewItem(size: Map<String, Int>, theme: Map<String, Any>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz): ViewItem {

    return TitleViewItem(
        id = "TITLE",
        text = translate["game_ipa_puzzle_screen_title"].orEmpty()
            .replace("\$param1", quiz.question.text)
            .replace("\$param2", quiz.question.ipaIncomplete)
            .with(BigForegroundColor(theme.colorOnSurface))
            .withFirst(quiz.question.text, BigBold, BigForegroundColor(theme.colorOnSurface))
            .withFirst(quiz.question.ipaIncomplete, BigBold, BigForegroundColor(theme.colorOnSurface))
            .withFirst("____", BigBold, BigForegroundColor(theme.colorError)).build(),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    )
}

fun getIPAPuzzleOptionViewItem(size: Map<String, Int>, theme: Map<String, Any>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, choose: String?) = quiz.answers.mapIndexed { index, answer ->

    ClickBigTextViewItem(
        id = "${Id.CHOOSE}_$index",
        data = answer,

        text = answer
            .with(BigBold, BigForegroundColor(theme.colorOnSurface)).build(),
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

fun getIPAPuzzleLoadingViewItem(size: Map<String, Int>, theme: Map<String, Any>): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val background = Background(
        cornerRadius = DP.DP_24,
        backgroundColor = theme.colorLoading
    )

    NoneBigTextViewItem(
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

    for (i in 0..3) NoneBigTextViewItem(
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