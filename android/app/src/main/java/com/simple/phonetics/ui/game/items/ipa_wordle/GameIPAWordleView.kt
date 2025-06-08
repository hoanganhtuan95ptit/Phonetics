package com.simple.phonetics.ui.game.items.ipa_wordle

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
import com.simple.phonetics.R
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.base.adapters.ImageStateViewItem
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isStart
import com.unknown.size.uitls.exts.getOrZero

private fun GameIPAWordleQuiz.Type.getName(translate: Map<String, String>): String = when (this) {

    GameIPAWordleQuiz.Type.IPA -> {
        translate["type_ipa"].orEmpty()
    }

    GameIPAWordleQuiz.Type.VOICE -> {
        translate["type_voice"].orEmpty()
    }

    else -> {
        translate["type_text"].orEmpty()
    }
}

fun getIPAWordleStateInfo(
    theme: Map<String, Int>,
    translate: Map<String, String>,
    quiz: GameIPAWordleQuiz,

    phoneticCode: String,
    isAnswerCorrect: Boolean
): GameItemViewModel.StateInfo {

    val param1 = quiz.answerType.getName(translate = translate).let {

        " $it "
    }

    val param2 = (when (quiz.questionType) {
        GameIPAWordleQuiz.Type.VOICE -> translate["type_voice"].orEmpty()
        GameIPAWordleQuiz.Type.TEXT -> quiz.question.text
        else -> quiz.question.ipa[phoneticCode]?.firstOrNull().orEmpty()
    }).let {

        " $it "
    }

    val param3 = (if (quiz.answerType == GameIPAWordleQuiz.Type.TEXT)
        quiz.question.text
    else
        quiz.question.ipa[phoneticCode]?.firstOrNull().orEmpty()).let {

        " $it "
    }

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

    val message = translate["game_ipa_wordle_screen_message_answer"].orEmpty()
        .replace("\$param1", param1)
        .replace("\$param2", param2)
        .replace("\$param3", param3)
        .replace("  ", " ")
        .with(ForegroundColorSpan(textColor))
        .with(param1, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorPrimary")))
        .with(param2, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorPrimary")))
        .with(param3, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorPrimary")))
        .trim()


    val list = arrayListOf<ViewItem>()

    NoneTextViewItem(
        id = "2",
        text = title.orEmpty()
            .with(ForegroundColorSpan(textColor)),
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
            .with(ForegroundColorSpan(theme.getOrTransparent("colorOnPrimary"))),
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

fun getIPAWordleTitleViewItem(theme: Map<String, Int>, translate: Map<String, String>, quiz: GameIPAWordleQuiz): ViewItem {

    val param1 = quiz.answerType.getName(translate = translate)
    val param2 = quiz.questionType.getName(translate = translate)

    return TitleViewItem(
        id = "TITLE",
        text = translate["game_ipa_wordle_screen_title"].orEmpty()
            .replace("\$param1", param1)
            .replace("\$param2", param2)
            .with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))
            .with(param1, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))
            .with(param2, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorPrimary"))),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    )
}

fun getIpaWordleQuestionViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    quiz: GameIPAWordleQuiz,

    listenState: ResultState<String>?,
    phoneticCode: String
) = if (quiz.questionType == GameIPAWordleQuiz.Type.VOICE) ImageStateViewItem(
    id = Id.LISTEN,

    data = quiz.question,

    image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
        R.drawable.ic_volume_24dp
    } else {
        R.drawable.ic_pause_24dp
    },
    imageMargin = Margin(
        margin = DP.DP_40,
    ),

    isLoading = listenState.isStart(),

    size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = (size.getOrZero("width") / 2.5f).toInt()
    )
) else NoneTextViewItem(
    id = "TEXT",
    data = quiz.question,
    text = if (quiz.questionType == GameIPAWordleQuiz.Type.TEXT) {
        quiz.question.text
    } else {
        quiz.question.ipa[phoneticCode]?.firstOrNull().orEmpty()
    }
        .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorOnSurface"))),
    textStyle = TextStyle(
        textSize = 30f,
        textGravity = Gravity.CENTER
    ),
    textSize = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.MATCH_PARENT
    ),
    size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = (size.getOrZero("width") / 2.5f).toInt()
    )
)

fun getIPAWordleOptionViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    quiz: GameIPAWordleQuiz,

    choose: Phonetic?,
    phoneticCode: String
) = quiz.answers.mapIndexed { index, phonetic ->

    val text = if (quiz.answerType == GameIPAWordleQuiz.Type.TEXT)
        phonetic.text
    else
        phonetic.ipa[phoneticCode]?.firstOrNull().orEmpty()


    ClickTextViewItem(
        id = "${Id.CHOOSE}_$index",
        data = phonetic,

        text = text
            .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorOnSurface"))),
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
            strokeColor = if (phonetic.text.equals(choose?.text, true)) {
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

fun getIPAWordleLoadingViewItem(size: Map<String, Int>, theme: Map<String, Int>): List<ViewItem> = arrayListOf<ViewItem>().apply {

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

    NoneTextViewItem(
        id = "LOADING_2",
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.MATCH_PARENT
        ),
        textMargin = Margin(
            marginVertical = DP.DP_8,
            marginHorizontal = DP.DP_100,
        ),
        textBackground = background,

        size = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = DP.DP_80
        ),
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
