package com.simple.phonetics.ui.game.items.ipa_wordle

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.exts.removeSpecialCharacters
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GameIPAWordleViewModel(
    private val stopReadingUseCase: StopReadingUseCase,
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : GameItemViewModel() {

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSourcesWithDiff(resourceSelected, phoneticCodeSelected) {

        val resourceSelected = resourceSelected.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        postValue(ResultState.Start)

        val param = GetPhoneticsRandomUseCase.Param(
            resource = resourceSelected,
            phoneticsCode = phoneticCodeSelected,

            limit = 4,
            textLengthMax = 10
        )

        val list = getPhoneticsRandomUseCase.execute(param = param).shuffled()

        if (list.isEmpty()) {
            logAnalytics("game_ipa_wordle_empty_${resourceSelected.removeSpecialCharacters().lowercase()}")
        }

        postValue(ResultState.Success(list))
    }

    val quiz: LiveData<GameIPAWordleQuiz> = combineSourcesWithDiff(isSupportReading, phoneticState) {

        val isSupportReading = isSupportReading.get()
        val phoneticState = phoneticState.get()

        val phonetics = phoneticState.toSuccess()?.data ?: return@combineSourcesWithDiff

        val typeRemoveList = arrayListOf<GameIPAWordleQuiz.Type>()

        // nếu không hỗ trợ phát âm thì bỏ ra khỏi danh sách
        if (!isSupportReading) {
            typeRemoveList.add(GameIPAWordleQuiz.Type.VOICE)
        }

        val answerType = listOf(GameIPAWordleQuiz.Type.TEXT, GameIPAWordleQuiz.Type.IPA).toMutableList().apply {

            removeAll(typeRemoveList)
        }.random()

        val questionType = GameIPAWordleQuiz.Type.entries.toMutableList().apply {

            remove(answerType)
            removeAll(typeRemoveList)
        }.random()

        val quiz = GameIPAWordleQuiz(
            answers = phonetics,
            answerType = answerType,

            question = phonetics.random(),
            questionType = questionType
        )

        postValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<Phonetic> = MediatorLiveData(null)

    val readingState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Success(""))

    val viewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(size, theme, translate, quiz, choose, readingState, phoneticState, phoneticCodeSelected) {

        val quiz = quiz.value
        val choose = choose.value
        val listenState = readingState.value
        val phoneticState = phoneticState.value
        val phoneticCodeSelected = phoneticCodeSelected.value ?: return@listenerSourcesWithDiff

        val size = size.value ?: return@listenerSourcesWithDiff
        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff


        if (quiz == null || phoneticState.isStart()) {

            postValue(getIPAWordleLoadingViewItem(size = size, theme = theme))
            return@listenerSourcesWithDiff
        }


        val list = arrayListOf<ViewItem>()

        getIPAWordleTitleViewItem(theme = theme, translate = translate, quiz = quiz).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_16))
            list.add(it)
        }

        getIpaWordleQuestionViewItem(size = size, theme = theme, quiz = quiz, listenState = listenState, phoneticCode = phoneticCodeSelected).let {

            list.add(it)
        }

        getIPAWordleOptionViewItem(size = size, theme = theme, quiz = quiz, choose = choose, phoneticCode = phoneticCodeSelected).let {

            list.add(SpaceViewItem(id = "SPACE_QUESTION_ANSWER"))
            list.addAll(it)
        }

        postValueIfActive(list)
    }

    val actionInfo: LiveData<ActionInfo> = listenerSourcesWithDiff(theme, translate, choose) {

        val choose = choose.value

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val isClickable = choose != null

        val textColor = if (isClickable) theme.getOrTransparent("colorOnPrimary") else theme.getOrTransparent("colorOnSurfaceVariant")

        val info = ActionInfo(
            text = translate["action_check"].orEmpty()
                .with(Bold, ForegroundColor(textColor)),
            isClickable = isClickable,

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = if (isClickable) theme.getOrTransparent("colorPrimary") else Color.TRANSPARENT,
                strokeWidth = (DP.DP_1 + DP.DP_05).toInt(),
                strokeColor = if (isClickable) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurfaceVariant"),
            )
        )

        postValue(info)
    }

    @VisibleForTesting
    val stateInfo: LiveData<StateInfo> = combineSourcesWithDiff(size, theme, translate, quiz, choose, phoneticCodeSelected, consecutiveCorrectAnswerEvent) {

        val theme = theme.get()
        val translate = translate.get()

        val quiz = quiz.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSourcesWithDiff

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val info = getIPAWordleStateInfo(
            theme = theme,
            translate = translate,
            quiz = quiz,

            phoneticCode = phoneticCodeSelected,
            isAnswerCorrect = isAnswerCorrect
        )

        postValue(info)
    }
    val stateInfoEvent: LiveData<Event<StateInfo>> = stateInfo.toEvent()


    val checkState: LiveData<ResultState<String>> = MediatorLiveData()


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

    fun startReading(s: String? = null) = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartReadingUseCase.Param(
            text = s ?: quiz.value?.question?.text ?: return@launch
        )

        readingState.postValue(ResultState.Start)

        var job: Job? = null

        job = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

            readingState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopListen() = viewModelScope.launch(handler + Dispatchers.IO) {

        stopReadingUseCase.execute()
    }
}