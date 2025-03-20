package com.simple.phonetics.ui.game.items.ipa_puzzle

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.state.ResultState
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameIPAPuzzleViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase
) : GameItemViewModel() {

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        getIpaStateAsyncUseCase.execute(GetIpaStateAsyncUseCase.Param(sync = false)).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSources(resourceSelected) {

        postDifferentValue(ResultState.Start)

        val list = getPhoneticsRandomUseCase.execute(GetPhoneticsRandomUseCase.Param(resource = resourceSelected.get(), limit = 1, textLengthMin = 3, textLengthMax = 10)).shuffled()

        postDifferentValue(ResultState.Success(list))
    }

    private val quiz: LiveData<Quiz> = combineSources(ipaState, phoneticState) {

        val ipaState = ipaState.value ?: return@combineSources
        val phoneticState = phoneticState.value ?: return@combineSources

        val ipaList = ipaState.toSuccess()?.data ?: return@combineSources
        val phonetic = phoneticState.toSuccess()?.data?.firstOrNull() ?: return@combineSources

        val ipaListWrap = ipaList
            .map { it.ipa.replace("/", "") }

        val questionIpaList = getQuestionIpaList(phonetic = phonetic, ipaList = ipaListWrap)

        val ipaMissing = questionIpaList.random()
        val ipaIncomplete = questionIpaList.map {

            if (it.first == ipaMissing.first) it.copy(
                second = "____"
            ) else {
                it
            }
        }

        val answers = ipaListWrap.toMutableList().apply {

            remove(ipaMissing.second)
        }.shuffled().subList(0, 3).toMutableList().apply {

            add(ipaMissing.second)
        }.shuffled()

        val question = Quiz.Question(
            text = phonetic.text,
            ipaMissing = ipaMissing.second,
            ipaIncomplete = ipaIncomplete.joinToString("") { it.second }
        )

        val quiz = Quiz(
            answers = answers,
            question = question,
        )

        postDifferentValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<String> = MediatorLiveData(null)

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, quiz, choose, ipaState, phoneticState) {

        val quiz = quiz.value ?: return@listenerSources
        val choose = choose.value
        val ipaState = ipaState.value
        val phoneticState = phoneticState.value

        val size = size.value ?: return@listenerSources
        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources


        if (ipaState.isStart() || phoneticState.isStart()) {

            postDifferentValue(getLoadingViewItem(theme = theme))
            return@listenerSources
        }


        val list = arrayListOf<ViewItem>()

        TitleViewItem(
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
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_16))
            list.add(it)
        }

        quiz.answers.mapIndexed { index, answer ->

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
        }.let {

            list.add(SpaceViewItem(id = "SPACE_OPTION_TITLE", height = DP.DP_24))
            list.addAll(it)
        }

        postDifferentValueIfActive(list)
    }

    val buttonInfo: LiveData<ButtonInfo> = listenerSources(theme, translate, choose) {

        val choose = choose.value

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val isClickable = choose != null

        val textColor = if (isClickable) theme.colorOnPrimary else theme.colorOnSurfaceVariant

        val info = ButtonInfo(
            text = translate["action_check"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(textColor)),
            isClickable = isClickable,

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = if (isClickable) theme.colorPrimary else Color.TRANSPARENT,
                strokeWidth = (DP.DP_1 + DP.DP_05).toInt(),
                strokeColor = if (isClickable) theme.colorPrimary else theme.colorOnSurfaceVariant,
            )
        )

        postDifferentValue(info)
    }


    @VisibleForTesting
    val stateInfo: LiveData<StateInfo> = combineSources(theme, translate, quiz, choose, consecutiveCorrectAnswerEvent) {

        val quiz = quiz.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSources


        val theme = theme.get()
        val translate = translate.get()

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val textColor = if (isAnswerCorrect) {
            theme.colorOnPrimaryVariant
        } else {
            theme.colorOnErrorVariant
        }

        val title = (if (isAnswerCorrect) {
            translate["title_answer_true"].orEmpty()
        } else {
            translate["title_answer_failed"].orEmpty()
        }).with(ForegroundColorSpan(textColor))

        val message = translate["game_ipa_puzzle_screen_message_answer"].orEmpty()
            .replace("\$param1", quiz.question.ipaMissing)
            .with(ForegroundColorSpan(textColor))
            .with(quiz.question.ipaMissing, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
            .trim()

        val positive = ButtonInfo(
            text = (if (isAnswerCorrect) {
                translate["action_continue"].orEmpty()
            } else {
                translate["action_retry"].orEmpty()
            }).with(ForegroundColorSpan(theme.colorOnPrimary)),
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

        val info = StateInfo(
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

        postDifferentValue(info)
    }
    val stateInfoEvent: LiveData<Event<StateInfo>> = stateInfo.toEvent()

    val checkState: LiveData<ResultState<String>> = MediatorLiveData()

    fun updateChoose(it: String?) {

        choose.postValue(it)
    }

    fun checkChoose() = viewModelScope.launch(handler + Dispatchers.IO) {

        val quiz = quiz.get()
        val choose = choose.get()

        checkState.postValue(ResultState.Start)

        val state = if (choose.equals(quiz.question.ipaMissing, true)) {
            ResultState.Success("")
        } else {
            ResultState.Failed(AppException("", ""))
        }

        checkState.postValue(state)
    }

    private fun getQuestionIpaList(phonetic: Phonetic, ipaList: List<String>): List<Pair<Int, String>> {

        val questionPhonetic = phonetic.ipa.toList()
            .flatMap { it.second }
            .first()
            .replace("/", "")

        var count = -1
        val questionIpaList = questionPhonetic.split("").associateBy {

            count++
        }.toList()

        val questionIpaValidateList = arrayListOf<Pair<Int, String>>()

        var index = 0
        while (index < questionIpaList.size) {

            val start = index

            var ipa = questionIpaList[start].second
            while (index + 1 < questionIpaList.size && (questionIpaList[index].second in listOf("ˈ") || (questionIpaList[index].second + questionIpaList[index + 1].second) in ipaList)) {

                ipa += questionIpaList[index + 1].second
                index++
            }

            questionIpaValidateList.add(start to ipa)
            index++
        }

        return questionIpaValidateList
    }

    private fun getLoadingViewItem(theme: AppTheme): List<ViewItem> = arrayListOf<ViewItem>().apply {

        val background = Background(
            cornerRadius = DP.DP_24,
            backgroundColor = theme.colorLoading
        )

        add(IpaDetailLoadingViewItem(id = "1", background = background))

        add(SpaceViewItem(id = "2", height = DP.DP_24))

        addAll(getPhoneticLoadingViewItem(theme = theme))
    }

    private class Quiz(
        val answers: List<String>,
        val question: Question,
    ) {

        class Question(
            val text: String,
            val ipaMissing: String,
            val ipaIncomplete: String,
        )
    }
}