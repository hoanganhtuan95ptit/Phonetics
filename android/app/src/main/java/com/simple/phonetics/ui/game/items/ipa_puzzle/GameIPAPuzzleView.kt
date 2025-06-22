package com.simple.phonetics.ui.game.items.ipa_puzzle

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
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichSpan
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.Id
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.unknown.size.uitls.exts.getOrZero

fun getIPAPuzzleStateInfo(size: Map<String, Int>, theme: Map<String, Int>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, isAnswerCorrect: Boolean): GameItemViewModel.StateInfo {

    val textColor = if (isAnswerCorrect) {
        theme.getOrTransparent("colorOnPrimaryVariant")
    } else {
        theme.getOrTransparent("colorOnErrorVariant")
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
        .with(ForegroundColor(textColor))
        .with(param1, Bold, ForegroundColor(theme.getOrTransparent("colorPrimary")))


    val list = arrayListOf<ViewItem>()

    NoneTextViewItem(
        id = "2",
        text = title.orEmpty()
            .with(ForegroundColor(textColor)),
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

    NoneTextViewItem(
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
            .with(ForegroundColor(theme.getOrTransparent("colorOnPrimary"))),
        background = Background(
            strokeWidth = 0,
            cornerRadius = DP.DP_16,
            backgroundColor = if (isAnswerCorrect) {
                theme.getOrTransparent("colorPrimary")
            } else {
                theme.getOrTransparent("colorError")
            }
        )
    )

    val info = GameItemViewModel.StateInfo(

        viewItemList = list,

        positive = positive,

        backgroundColor = if (isAnswerCorrect) {
            theme.getOrTransparent("colorPrimaryVariant")
        } else {
            theme.getOrTransparent("colorErrorVariant")
        }
    )

    return info
}

fun getIPAPuzzleTitleViewItem(size: Map<String, Int>, theme: Map<String, Int>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz): ViewItem {

    return TitleViewItem(
        id = "TITLE",
        text = translate["game_ipa_puzzle_screen_title"].orEmpty()
            .replace("\$param1", quiz.question.text)
            .replace("\$param2", quiz.question.ipaIncomplete)
            .with(ForegroundColor(theme.getOrTransparent("colorOnSurface")))
            .with(quiz.question.text, Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface")))
            .with(quiz.question.ipaIncomplete, Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface")))
            .with("____", Bold, ForegroundColor(theme.getOrTransparent("colorError"))),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    )
}

fun getIPAPuzzleOptionViewItem(size: Map<String, Int>, theme: Map<String, Int>, translate: Map<String, String>, quiz: GameIPAPuzzleQuiz, choose: String?) = quiz.answers.mapIndexed { index, answer ->

    ClickTextViewItem(
        id = "${Id.CHOOSE}_$index",
        data = answer,

        text = answer
            .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
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
                theme.getOrTransparent("colorPrimary")
            } else {
                theme.getOrTransparent("colorOnSurfaceVariant")
            }
        ),

        size = Size(
            width = (size.getOrZero("width") - 2 * DP.DP_8) / 2,
            height = (size.getOrZero("width") - 2 * DP.DP_8) / 2 + DP.DP_16
        ),
        padding = Padding(
            paddingVertical = DP.DP_8,
            paddingHorizontal = DP.DP_8
        )
    )
}

fun getIPAPuzzleLoadingViewItem(size: Map<String, Int>, theme: Map<String, Int>): List<ViewItem> = arrayListOf<ViewItem>().apply {

    val background = Background(
        cornerRadius = DP.DP_24,
        backgroundColor = theme.getOrTransparent("colorLoading")
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
            width = (size.getOrZero("width") - 2 * DP.DP_8) / 2,
            height = (size.getOrZero("width") - 2 * DP.DP_8) / 2 + DP.DP_16
        ),
    ).let {

        add(it)
    }
}