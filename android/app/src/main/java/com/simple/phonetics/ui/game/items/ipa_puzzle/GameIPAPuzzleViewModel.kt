package com.simple.phonetics.ui.game.items.ipa_puzzle

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.coreapp.ui.view.Background
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
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.dao.entities.Ipa
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.game.items.GameItemViewModel
import com.simple.state.ResultState
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameIPAPuzzleViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase
) : GameItemViewModel() {

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        getIpaStateAsyncUseCase.execute(GetIpaStateAsyncUseCase.Param(sync = false)).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSources(resourceSelected, phoneticCodeSelected) {

        val resourceSelected = resourceSelected.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        postDifferentValue(ResultState.Start)

        val param = GetPhoneticsRandomUseCase.Param(
            resource = resourceSelected,
            phoneticsCode = phoneticCodeSelected,

            limit = 1,
            textLengthMin = 3,
            textLengthMax = 10
        )

        val list = getPhoneticsRandomUseCase.execute(param = param).shuffled()

        postDifferentValue(ResultState.Success(list))
    }

    private val quiz: LiveData<GameIPAPuzzleQuiz> = combineSources(ipaState, phoneticState, phoneticCodeSelected) {

        val ipaState = ipaState.value ?: return@combineSources
        val phoneticState = phoneticState.value ?: return@combineSources
        val phoneticCodeSelected = phoneticCodeSelected.value ?: return@combineSources

        val ipaList = ipaState.toSuccess()?.data ?: return@combineSources
        val phonetic = phoneticState.toSuccess()?.data?.firstOrNull() ?: return@combineSources

        val ipaListWrap = ipaList
            .map { it.ipa.replace("/", "") }

        val questionIpaList = getQuestionIpaList(ipaList = ipaListWrap, phonetic = phonetic, phoneticCode = phoneticCodeSelected)

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

        val question = GameIPAPuzzleQuiz.Question(
            text = phonetic.text,
            ipaMissing = ipaMissing.second,
            ipaIncomplete = ipaIncomplete.joinToString("") { it.second }
        )

        val quiz = GameIPAPuzzleQuiz(
            answers = answers,
            question = question,
        )

        postDifferentValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<String> = MediatorLiveData(null)

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, quiz, choose, ipaState, phoneticState) {

        val quiz = quiz.value
        val choose = choose.value
        val ipaState = ipaState.value
        val phoneticState = phoneticState.value

        val size = size.value ?: return@listenerSources
        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources


        if (quiz == null || ipaState.isStart() || phoneticState.isStart()) {

            postDifferentValue(getIPAPuzzleLoadingViewItem(size = size, theme = theme))
            return@listenerSources
        }


        val list = arrayListOf<ViewItem>()

        getIPAPuzzleTitleViewItem(size = size, theme = theme, translate = translate, quiz = quiz).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE", height = DP.DP_16))
            list.add(it)
        }

        getIPAPuzzleOptionViewItem(size = size, theme = theme, translate = translate, quiz = quiz, choose = choose).let {

            list.add(SpaceViewItem(id = "SPACE_OPTION_TITLE", height = DP.DP_24))
            list.addAll(it)
        }

        postDifferentValueIfActive(list)
    }

    val actionInfo: LiveData<ActionInfo> = listenerSources(theme, translate, choose) {

        val choose = choose.value

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val isClickable = choose != null

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
    val stateInfo: LiveData<StateInfo> = combineSources(size, theme, translate, quiz, choose, consecutiveCorrectAnswerEvent) {

        val quiz = quiz.get()
        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSources


        val size = size.get()
        val theme = theme.get()
        val translate = translate.get()

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val info = getIPAPuzzleStateInfo(
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

    private fun getQuestionIpaList(phonetic: Phonetic, ipaList: List<String>, phoneticCode: String): List<Pair<Int, String>> {

        val questionPhonetic = phonetic.ipa[phoneticCode]?.firstOrNull().orEmpty()
            .replace("/", "")


        val questionIpaList = questionPhonetic.split("").filter {

            it !in listOf("", "‍")
        }.mapIndexed { index, s ->

            index to s
        }


        val questionIpaValidateList = arrayListOf<Pair<Int, String>>()

        var index = 0
        while (index < questionIpaList.size) {

            val start = index

            var ipa = questionIpaList[start].second
            while (index + 1 < questionIpaList.size && (questionIpaList[index].second in listOf("ˈ", "ˌ") || (questionIpaList[index].second + questionIpaList[index + 1].second) in ipaList)) {

                ipa += questionIpaList[index + 1].second
                index++
            }

            questionIpaValidateList.add(start to ipa)
            index++
        }

        return questionIpaValidateList
    }
}