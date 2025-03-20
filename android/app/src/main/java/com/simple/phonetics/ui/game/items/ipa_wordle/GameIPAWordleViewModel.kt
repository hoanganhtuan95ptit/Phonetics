package com.simple.phonetics.ui.game.items.ipa_wordle

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
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
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
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.voice.StartListenUseCase
import com.simple.phonetics.domain.usecase.voice.StopListenUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.base.adapters.ImageStateViewItem
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getPhoneticLoadingViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GameIPAWordleViewModel(
    private val stopListenUseCase: StopListenUseCase,
    private val startListenUseCase: StartListenUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase
) : GameItemViewModel() {

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSources(resourceSelected) {

        postDifferentValue(ResultState.Start)

        val list = getPhoneticsRandomUseCase.execute(GetPhoneticsRandomUseCase.Param(resource = resourceSelected.get(), limit = 4, textLengthMax = 10)).shuffled()

        postDifferentValue(ResultState.Success(list))
    }

    private val quiz: LiveData<Quiz> = combineSources(listenEnable, phoneticState) {

        val listenEnable = listenEnable.value ?: return@combineSources
        val phoneticState = phoneticState.value ?: return@combineSources

        val phonetics = phoneticState.toSuccess()?.data ?: return@combineSources

        val typeRemoveList = arrayListOf<Quiz.Type>()

        // nếu không hỗ trợ phát âm thì bỏ ra khỏi danh sách
        if (!listenEnable) {
            typeRemoveList.add(Quiz.Type.VOICE)
        }

        val answerType = listOf(Quiz.Type.TEXT, Quiz.Type.IPA).toMutableList().apply {

            removeAll(typeRemoveList)
        }.random()

        val questionType = Quiz.Type.entries.toMutableList().apply {

            remove(answerType)
            removeAll(typeRemoveList)
        }.random()

        val quiz = Quiz(
            answers = phonetics,
            answerType = answerType,

            question = phonetics.random(),
            questionType = questionType
        )

        postDifferentValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<Phonetic> = MediatorLiveData(null)

    val listenState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Success(""))

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, quiz, choose, listenState, phoneticState) {

        val quiz = quiz.value ?: return@listenerSources
        val choose = choose.value
        val listenState = listenState.value
        val phoneticState = phoneticState.value

        val size = size.value ?: return@listenerSources
        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources


        if (phoneticState.isStart()) {

            postDifferentValue(getLoadingViewItem(theme = theme))
            return@listenerSources
        }


        val list = arrayListOf<ViewItem>()

        val param1 = quiz.answerType.getName(translate = translate)
        val param2 = quiz.questionType.getName(translate = translate)

        TitleViewItem(
            id = "TITLE",
            text = translate["game_ipa_wordle_screen_title"].orEmpty()
                .replace("\$param1", param1)
                .replace("\$param2", param2)
                .with(ForegroundColorSpan(theme.colorOnSurface))
                .with(param1, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface))
                .with(param2, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary)),
            textMargin = Margin(
                marginHorizontal = DP.DP_8
            )
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_16))
            list.add(it)
        }

        (if (quiz.questionType == Quiz.Type.VOICE) ImageStateViewItem(
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
                height = (size.width / 2.5f).toInt()
            )
        ) else NoneTextViewItem(
            id = "TEXT",
            data = quiz.question,
            text = (if (quiz.questionType == Quiz.Type.TEXT)
                quiz.question.text
            else
                quiz.question.ipa.flatMap { it.value }.first())
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
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
                height = (size.width / 2.5f).toInt()
            )
        )).let {

            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_QUESTION_ANSWER"))
        }

        quiz.answers.mapIndexed { index, phonetic ->

            val text = if (quiz.answerType == Quiz.Type.TEXT)
                phonetic.text
            else
                phonetic.ipa.flatMap { it.value }.first()


            ClickTextViewItem(
                id = "${Id.CHOOSE}_$index",
                data = phonetic,

                text = text
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
                    strokeColor = if (phonetic.text.equals(choose?.text, true)) {
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


    val checkState: LiveData<ResultState<String>> = MediatorLiveData()

    val stateInfo: LiveData<StateInfo> = combineSources(theme, translate, quiz, choose, consecutiveCorrectAnswerEvent) {

        val quiz = quiz.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSources


        val theme = theme.get()
        val translate = translate.get()

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val param1 = quiz.answerType.getName(translate = translate).let {

            " $it "
        }

        val param2 = (when (quiz.questionType) {
            Quiz.Type.VOICE -> translate["type_voice"].orEmpty()
            Quiz.Type.TEXT -> quiz.question.text
            else -> quiz.question.ipa.flatMap { it.value }.first()
        }).let {

            " $it "
        }

        val param3 = (if (quiz.answerType == Quiz.Type.TEXT)
            quiz.question.text
        else
            quiz.question.ipa.flatMap { it.value }.first()).let {

            " $it "
        }

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

        val message = translate["game_ipa_wordle_screen_message_answer"].orEmpty()
            .replace("\$param1", param1)
            .replace("\$param2", param2)
            .replace("\$param3", param3)
            .replace("  ", " ")
            .with(ForegroundColorSpan(textColor))
            .with(param1, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
            .with(param2, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
            .with(param3, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
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

    fun updateChoose(phonetic: Phonetic?) {

        choose.postValue(phonetic)
    }

    fun checkChoose() = viewModelScope.launch(handler + Dispatchers.IO) {

        val quiz = quiz.get()
        val choose = choose.get()

        checkState.postValue(ResultState.Start)

        val state = if (choose.text.equals(quiz.question.text, true)) {
            ResultState.Success("")
        } else {
            ResultState.Failed(AppException("", ""))
        }

        checkState.postValue(state)
    }

    fun startListen(voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        listenState.postValue(ResultState.Start)

        val param = StartListenUseCase.Param(
            text = quiz.value?.question?.text.orEmpty(),

            languageCode = inputLanguage.value?.id ?: Language.EN,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

        var job: Job? = null

        job = startListenUseCase.execute(param).launchCollect(viewModelScope) { state ->

            listenState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopListen() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopListenUseCase.execute()
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

    private fun Quiz.Type.getName(translate: Map<String, String>): String = when (this) {

        Quiz.Type.IPA -> {
            translate["type_ipa"].orEmpty()
        }

        Quiz.Type.VOICE -> {
            translate["type_voice"].orEmpty()
        }

        else -> {
            translate["type_text"].orEmpty()
        }
    }

    private class Quiz(
        val answers: List<Phonetic>,
        val answerType: Type,

        val question: Phonetic,
        val questionType: Type,
    ) {

        enum class Type {
            VOICE, IPA, TEXT
        }
    }
}