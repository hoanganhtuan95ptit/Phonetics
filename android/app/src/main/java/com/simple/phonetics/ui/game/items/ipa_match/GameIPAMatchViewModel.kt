@file:Suppress("FunctionName")

package com.simple.phonetics.ui.game.items.ipa_match

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.toArrayList
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.handler
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
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.state.toSuccess
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

class GameIPAMatchViewModel(
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : GameItemViewModel() {

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<com.simple.phonetic.entities.Phonetic>>> = combineSourcesWithDiff(resourceSelected, phoneticCodeSelected) {

        val resourceSelected = resourceSelected.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        postValue(ResultState.Start)

        val param = GetPhoneticsRandomUseCase.Param(
            resource = resourceSelected,
            phoneticsCode = phoneticCodeSelected,

            limit = 4,
            textLengthMin = 3,
            textLengthMax = 10
        )

        val list = getPhoneticsRandomUseCase.execute(param = param).shuffled()

        postValue(ResultState.Success(list))
    }

    val quiz: LiveData<GameIPAMatchQuiz> = combineSourcesWithDiff(isSupportReading, phoneticState) {

        val isSupportReading = isSupportReading.value ?: return@combineSourcesWithDiff
        val phoneticState = phoneticState.value ?: return@combineSourcesWithDiff

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSourcesWithDiff


        val typeRemoveList = arrayListOf<GameIPAMatchQuiz.Option.Type>()

        typeRemoveList.add(GameIPAMatchQuiz.Option.Type.NONE)

        // nếu không hỗ trợ phát âm thì bỏ ra khỏi danh sách
        if (!isSupportReading) {
            typeRemoveList.add(GameIPAMatchQuiz.Option.Type.VOICE)
        }


        val typeFirst = GameIPAMatchQuiz.Option.Type.entries.toMutableList().apply {

            removeAll(typeRemoveList)
        }.random()

        val typeSecond = GameIPAMatchQuiz.Option.Type.entries.toMutableList().apply {

            remove(typeFirst)
            removeAll(typeRemoveList)
        }.random()


        val phoneticListShuffled = phoneticList.shuffled()

        val match = phoneticList.mapIndexed { index, phonetic ->
            GameIPAMatchQuiz.Option(type = typeFirst, phonetic = phonetic) to GameIPAMatchQuiz.Option(type = typeSecond, phonetic = phoneticListShuffled[index])
        }

        val quiz = GameIPAMatchQuiz(
            match = match
        )


        postValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<List<Pair<GameIPAMatchQuiz.Option?, GameIPAMatchQuiz.Option?>>> = combineSourcesWithDiff(phoneticState) {

        val phoneticState = phoneticState.get()

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSourcesWithDiff

        postValue(phoneticList.map { null to null })
    }

    @VisibleForTesting
    val warning: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val readingState: LiveData<HashMap<GameIPAMatchPair, ResultState<String>>> = MediatorLiveData(hashMapOf())

    val viewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(size, theme, translate, quiz, choose, warning, readingState, phoneticState, phoneticCodeSelected) {

        val quiz = quiz.value
        val choose = choose.value
        val warning = warning.value ?: false
        val listenState = readingState.value.orEmpty()
        val phoneticState = phoneticState.value
        val phoneticCodeSelected = phoneticCodeSelected.value ?: return@listenerSourcesWithDiff

        val size = size.value ?: return@listenerSourcesWithDiff
        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        if (quiz == null || phoneticState.isStart()) {

            postValue(getIPAMatchLoadingViewItem(size = size, theme = theme))
            return@listenerSourcesWithDiff
        }


        val list = arrayListOf<ViewItem>()

        val chooseList = choose.orEmpty().flatMap {

            listOf(it.first, it.second)
        }

        getIPAMatchQuestionViewItem(size = size, theme = theme, translate = translate, quiz = quiz, chooseList = chooseList, warning = warning, listenState = listenState, phoneticCode = phoneticCodeSelected).let {

            list.addAll(it)
        }

        getIPAMatchOptionViewItem(size = size, theme = theme, translate = translate, quiz = quiz, chooseList = chooseList, warning = warning, listenState = listenState, phoneticCode = phoneticCodeSelected).let {

            list.addAll(it)
        }

        postValueIfActive(list)
    }

    val actionInfo: LiveData<ActionInfo> = listenerSourcesWithDiff(theme, translate, choose) {

        val choose = choose.value.orEmpty().flatMap { listOf<Any?>(it.first, it.second) }

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val isClickable = choose.isNotEmpty() && !choose.any { it == null }

        val textColor = if (isClickable) theme.colorOnPrimary else theme.colorOnSurfaceVariant

        val info = ActionInfo(
            text = translate["action_check"].orEmpty()
                .with(Bold, ForegroundColor(textColor)),
            isClickable = isClickable,

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = if (isClickable) theme.colorPrimary else Color.TRANSPARENT,
                strokeWidth = (DP.DP_1 + DP.DP_05).toInt(),
                strokeColor = if (isClickable) theme.colorPrimary else theme.colorOnSurfaceVariant,
            )
        )

        postValue(info)
    }


    @VisibleForTesting
    val stateInfo: LiveData<StateInfo> = combineSourcesWithDiff(size, theme, translate, quiz, consecutiveCorrectAnswerEvent) {

        val quiz = quiz.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSourcesWithDiff

        val size = size.get()
        val theme = theme.get()
        val translate = translate.get()

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val info = getIPAMatchStateInfo(
            size = size,
            theme = theme,
            translate = translate,
            quiz = quiz,
            isAnswerCorrect = isAnswerCorrect
        )

        postValue(info)
    }
    val stateInfoEvent: LiveData<Event<StateInfo>> = stateInfo.toEvent()

    val checkState: LiveData<ResultState<String>> = MediatorLiveData()


    fun resetChoose() {

        choose.postValue(choose.value.orEmpty().map { null to null })
    }

    fun updateChoose(it: GameIPAMatchPair) {

        val type = it.newType ?: return
        val option = it.option ?: return

        if (type == GameIPAMatchQuiz.Option.Type.NONE) return

        val quiz = quiz.value?.match ?: return
        val chooseList = choose.value.orEmpty()

        val fromChoose = option in chooseList.flatMap { listOf(it.first, it.second) }

        val newList = if (fromChoose) chooseList.map {

            if (it.first == option) {
                null to it.second
            } else if (it.second == option) {
                it.first to null
            } else {
                it.first to it.second
            }
        } else {

            val map = chooseList.toArrayList()

            quiz.forEach { entry ->

                if (entry.first == option) {
                    val index = max(0, map.indexOfFirst { it.second == null })
                    val key = map.getOrNull(index)?.first
                    if (map.size > index) map.removeAt(index)
                    map.add(index, key to option)
                } else if (entry.second == option) {
                    val index = max(0, map.indexOfFirst { it.first == null })
                    val value = map.getOrNull(index)?.second
                    if (map.size > index) map.removeAt(index)
                    map.add(index, option to value)
                }
            }

            map
        }

        choose.postValue(newList)
    }

    fun checkChoose() = viewModelScope.launch(handler + Dispatchers.IO) {

        val choose = choose.get()

        checkState.postValue(ResultState.Start)

        val state = if (choose.all { it.first?.phonetic?.text.equals(it.second?.phonetic?.text, true) }) {
            ResultState.Success("")
        } else {
            ResultState.Failed(AppException("", ""))
        }

        checkState.postValue(state)
    }

    fun updateWaring(it: Boolean) {

        warning.postValue(it)
    }

    fun startReading(data: GameIPAMatchPair) = viewModelScope.launch(handler + Dispatchers.IO) {

        val map = readingState.value ?: return@launch

        val param = StartReadingUseCase.Param(
            text = data.option?.phonetic?.text ?: return@launch
        )


        map[data] = ResultState.Start
        readingState.postValue(map)

        var job: Job? = null

        job = startReadingUseCase.execute(param).launchCollect(viewModelScope) { state ->

            map[data] = state
            readingState.postValue(map)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }
}