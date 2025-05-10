@file:Suppress("FunctionName")

package com.simple.phonetics.ui.game.items.ipa_match

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.toArrayList
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

class GameIPAMatchViewModel(
    private val startReadingUseCase: StartReadingUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : GameItemViewModel() {

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSources(resourceSelected, phoneticCodeSelected) {

        val resourceSelected = resourceSelected.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        postDifferentValue(ResultState.Start)

        val param = GetPhoneticsRandomUseCase.Param(
            resource = resourceSelected,
            phoneticsCode = phoneticCodeSelected,

            limit = 4,
            textLengthMin = 3,
            textLengthMax = 10
        )

        val list = getPhoneticsRandomUseCase.execute(param = param).shuffled()

        postDifferentValue(ResultState.Success(list))
    }

    @VisibleForTesting
    val quiz: LiveData<GameIPAMatchQuiz> = combineSources(isSupportReading, phoneticState) {

        val isSupportReading = isSupportReading.value ?: return@combineSources
        val phoneticState = phoneticState.value ?: return@combineSources

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSources


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


        postDifferentValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<List<Pair<GameIPAMatchQuiz.Option?, GameIPAMatchQuiz.Option?>>> = combineSources(phoneticState) {

        val phoneticState = phoneticState.get()

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSources

        postDifferentValue(phoneticList.map { null to null })
    }

    @VisibleForTesting
    val warning: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val readingState: LiveData<HashMap<GameIPAMatchPair, ResultState<String>>> = MediatorLiveData(hashMapOf())

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, quiz, choose, warning, readingState, phoneticState, phoneticCodeSelected) {

        val quiz = quiz.value
        val choose = choose.value
        val warning = warning.value ?: false
        val listenState = readingState.value.orEmpty()
        val phoneticState = phoneticState.value
        val phoneticCodeSelected = phoneticCodeSelected.value ?: return@listenerSources

        val size = size.value ?: return@listenerSources
        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        if (quiz == null || phoneticState.isStart()) {

            postDifferentValue(getIPAMatchLoadingViewItem(size = size, theme = theme))
            return@listenerSources
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

        postDifferentValueIfActive(list)
    }

    val actionInfo: LiveData<ActionInfo> = listenerSources(theme, translate, choose) {

        val choose = choose.value

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val isClickable = !choose.orEmpty().flatMap { listOf<Any?>(it.first, it.second) }.any { it == null }

        val textColor = if (isClickable) theme.colorOnPrimary else theme.colorOnSurfaceVariant

        val info = ActionInfo(
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
    val stateInfo: LiveData<StateInfo> = combineSources(size, theme, translate, quiz, consecutiveCorrectAnswerEvent) {

        val quiz = quiz.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSources

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

        postDifferentValue(info)
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

        warning.postDifferentValue(it)
    }

    fun startListen(data: GameIPAMatchPair, voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        val map = readingState.value ?: return@launch

        val param = StartReadingUseCase.Param(
            text = data.option?.phonetic?.text ?: return@launch,

            voiceId = voiceId,

            voiceSpeed = voiceSpeed
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