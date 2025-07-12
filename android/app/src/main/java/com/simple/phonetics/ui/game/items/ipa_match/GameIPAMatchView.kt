package com.simple.phonetics.ui.game.items.ipa_match

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
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
import com.simple.coreapp.utils.ext.with
import com.simple.phonetics.R
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.base.adapters.ImageStateViewItem
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isStart
import com.unknown.size.uitls.exts.getOrZero

fun getIPAMatchStateInfo(size: Map<String, Int>, theme: Map<String, Int>, translate: Map<String, String>, quiz: GameIPAMatchQuiz, isAnswerCorrect: Boolean): GameItemViewModel.StateInfo {

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

    val message = if (isAnswerCorrect) {
        translate["game_ipa_match_screen_message_answer_correct"]
    } else {
        translate["game_ipa_match_screen_message_answer_failed"]
    }


    val list = arrayListOf<ViewItem>()

    NoneTextViewItem(
        id = "2",
        text = title.orEmpty()
            .with(Bold, ForegroundColor(textColor)),
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
        text = message.orEmpty()
            .with(ForegroundColor(textColor)),
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

fun getIPAMatchQuestionViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    translate: Map<String, String>,

    quiz: GameIPAMatchQuiz,
    warning: Boolean,
    chooseList: List<GameIPAMatchQuiz.Option?>,
    listenState: Map<GameIPAMatchPair, ResultState<String>>,
    phoneticCode: String,
) = arrayListOf<ViewItem>().apply {

    val chooseViewItemList = chooseList.mapIndexed { index, option ->

        val optionPair = chooseList.getOrNull(if (index % 2 == 0) index + 1 else index - 1)

        if (option == null) OptionViewItem(
            size = size,
            theme = theme,
            background = Background(strokeColor = theme.getOrTransparent("colorPrimary"), strokeDashEnable = true),

            data = GameIPAMatchPair(GameIPAMatchQuiz.Option(GameIPAMatchQuiz.Option.Type.NONE, phonetic = Phonetic("$index")), null),
            listenState = listenState,
            phoneticCode = phoneticCode
        ) else OptionViewItem(
            size = size,
            theme = theme,
            background = Background(strokeColor = if (warning && !optionPair?.phonetic?.text.equals(option.phonetic.text, true)) theme.getOrTransparent("colorOnErrorVariant") else theme.getOrTransparent("colorPrimary"), strokeDashEnable = false),

            data = GameIPAMatchPair(option = option, newType = option.type),
            listenState = listenState,
            phoneticCode = phoneticCode
        )
    }

    TitleViewItem(
        id = "TITLE",
        text = translate["game_ipa_match_screen_title"].orEmpty()
            .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    ).let {

        add(SpaceViewItem(id = "SPACE_CHOOSE_0", height = DP.DP_16))
        add(it)

        add(SpaceViewItem(id = "SPACE_CHOOSE_1", height = DP.DP_16))
        addAll(chooseViewItemList)
    }
}

fun getIPAMatchOptionViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    translate: Map<String, String>,

    quiz: GameIPAMatchQuiz,
    warning: Boolean,
    chooseList: List<GameIPAMatchQuiz.Option?>,
    listenState: Map<GameIPAMatchPair, ResultState<String>>,
    phoneticCode: String
) = arrayListOf<ViewItem>().apply {

    val optionViewItemList = quiz.match.flatMap {

        listOf(it.first, it.second)
    }.map {

        OptionViewItem(
            size = size,
            theme = theme,
            background = Background(strokeColor = theme.getOrTransparent("colorOnSurfaceVariant"), strokeDashEnable = true),

            data = GameIPAMatchPair(option = it, newType = if (it in chooseList) GameIPAMatchQuiz.Option.Type.NONE else it.type),
            listenState = listenState,
            phoneticCode = phoneticCode
        )
    }

    TitleViewItem(
        id = "TITLE_OPTION_CHO0SE",
        text = translate["game_ipa_match_screen_title_choose"].orEmpty()
            .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        textMargin = Margin(
            marginHorizontal = DP.DP_8
        )
    ).let {

        add(SpaceViewItem(id = "SPACE_OPTION_1", height = DP.DP_24))
        add(it)

        add(SpaceViewItem(id = "SPACE_OPTION_2", height = DP.DP_8))
        addAll(optionViewItemList)
    }
}

fun getIPAMatchLoadingViewItem(size: Map<String, Int>, theme: Map<String, Int>): List<ViewItem> = arrayListOf<ViewItem>().apply {

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

    for (i in 0..7) NoneTextViewItem(
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
            height = DP.DP_56
        ),
    ).let {

        add(it)
    }
}


private fun OptionViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    background: Background,

    data: GameIPAMatchPair,
    listenState: Map<GameIPAMatchPair, ResultState<String>>,
    phoneticCode: String,
): ViewItem {

    return if (data.newType == GameIPAMatchQuiz.Option.Type.VOICE) {
        OptionVoiceViewItem(size = size, theme = theme, background = background, data = data, listenState = listenState, phoneticCode = phoneticCode)
    } else {
        OptionTextViewItem(size = size, theme = theme, background = background, data = data, listenState = listenState, phoneticCode = phoneticCode)
    }
}

private fun OptionTextViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    background: Background,

    data: GameIPAMatchPair,
    listenState: Map<GameIPAMatchPair, ResultState<String>>,
    phoneticCode: String,
): ViewItem {

    val phonetic = data.option?.phonetic

    val text = if (data.newType == GameIPAMatchQuiz.Option.Type.TEXT) {
        phonetic?.text
    } else if (data.newType == GameIPAMatchQuiz.Option.Type.IPA) {
        (phonetic?.ipa?.get(phoneticCode) ?: phonetic?.ipa?.flatMap { it.value })?.firstOrNull()
    } else {
        ""
    }

    return ClickTextViewItem(
        id = "${phonetic?.text}_${data.option?.type?.name}_${data.newType?.name}",
        data = data,

        text = text.orEmpty()
            .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        textStyle = TextStyle(
            textSize = 16f,
            textGravity = Gravity.CENTER
        ),
        textSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.MATCH_PARENT
        ),
        textBackground = background,

        size = Size(
            width = (size.getOrZero("width") - 2 * DP.DP_8) / 2,
            height = DP.DP_56
        ),
        padding = Padding(
            paddingVertical = DP.DP_8,
            paddingHorizontal = DP.DP_8
        )
    )
}

private fun OptionVoiceViewItem(
    size: Map<String, Int>,
    theme: Map<String, Int>,
    background: Background,

    data: GameIPAMatchPair,
    listenState: Map<GameIPAMatchPair, ResultState<String>>,
    phoneticCode: String,
): ViewItem {

    val state = listenState[data]

    val image = if (state == null || state.isStart() || state.isCompleted()) {
        R.drawable.ic_volume_24dp
    } else {
        R.drawable.ic_pause_24dp
    }

    return ImageStateViewItem(
        id = "${data.option?.phonetic?.text}_${data.option?.type?.name}_${data.newType?.name}",
        data = data,

        image = image,
        imageSize = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.MATCH_PARENT
        ),
        imagePadding = Padding(
            paddingVertical = DP.DP_4,
            paddingHorizontal = DP.DP_4
        ),
        imageBackground = background,

        isLoading = state.isStart(),

        size = Size(
            width = (size.getOrZero("width") - 2 * DP.DP_8) / 2,
            height = DP.DP_56
        ),
        padding = Padding(
            paddingVertical = DP.DP_8,
            paddingHorizontal = DP.DP_8
        )
    )
}

private fun Background(
    strokeColor: Int = Color.TRANSPARENT,
    strokeDashEnable: Boolean = false
) = Background(
    cornerRadius = DP.DP_16,
    strokeWidth = DP.DP_2,
    strokeColor = strokeColor,
    strokeDashGap = if (strokeDashEnable) DP.DP_8 else 0,
    strokeDashWidth = if (strokeDashEnable) DP.DP_8 else 0
)