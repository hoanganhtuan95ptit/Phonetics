@file:Suppress("FunctionName")

package com.simple.phonetics.ui.game.items.ipa_match

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.toArrayList
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
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
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsRandomUseCase
import com.simple.phonetics.domain.usecase.voice.StartListenUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.adapters.ImageStateViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.ipa.detail.adapters.IpaDetailLoadingViewItem
import com.simple.phonetics.utils.AppSize
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

class GameIPAMatchViewModel(
    private val startListenUseCase: StartListenUseCase,
    private val getPhoneticsRandomUseCase: GetPhoneticsRandomUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }


    @VisibleForTesting
    val listenEnable: LiveData<Boolean> = MediatorLiveData()

    @VisibleForTesting
    val resourceSelected: LiveData<Word.Resource> = MediatorLiveData()


    @VisibleForTesting
    val phoneticState: LiveData<ResultState<List<Phonetic>>> = combineSources(resourceSelected) {

        postDifferentValue(ResultState.Start)

        val list = getPhoneticsRandomUseCase.execute(GetPhoneticsRandomUseCase.Param(resource = resourceSelected.get(), limit = 4, textLengthMin = 3, textLengthMax = 10)).shuffled()

        postDifferentValue(ResultState.Success(list))
    }


    private val quiz: LiveData<Quiz> = combineSources(listenEnable, phoneticState) {

        val listenEnable = listenEnable.value ?: return@combineSources
        val phoneticState = phoneticState.value ?: return@combineSources

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSources


        val typeRemoveList = arrayListOf<Type>()

        typeRemoveList.add(Type.NONE)

        // nếu không hỗ trợ phát âm thì bỏ ra khỏi danh sách
        if (!listenEnable) {
            typeRemoveList.add(Type.VOICE)
        }


        val typeFirst = Type.entries.toMutableList().apply {

            removeAll(typeRemoveList)
        }.random()

        val typeSecond = Type.entries.toMutableList().apply {

            remove(typeFirst)
            removeAll(typeRemoveList)
        }.random()

        val quiz = Quiz(
            match = phoneticList.map { Option(type = typeFirst, phonetic = it) to Option(type = typeSecond, phonetic = it) },
        )

        postDifferentValue(quiz)
    }

    @VisibleForTesting
    val choose: LiveData<List<Pair<Option?, Option?>>> = combineSources(phoneticState) {

        val phoneticState = phoneticState.get()

        val phoneticList = phoneticState.toSuccess()?.data ?: return@combineSources

        postDifferentValue(phoneticList.map { null to null })
    }

    val warning: LiveData<Boolean> = MediatorLiveData(false)

    val listenState: LiveData<HashMap<Pair<Option, Type>, ResultState<String>>> = MediatorLiveData(hashMapOf())

    val viewItemList: LiveData<List<ViewItem>> = listenerSources(size, theme, translate, quiz, choose, warning, listenState, phoneticState) {

        val quiz = quiz.value ?: return@listenerSources
        val choose = choose.value
        val phoneticState = phoneticState.value

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        if (phoneticState.isStart()) {

            postDifferentValue(getLoadingViewItem(theme = theme))
            return@listenerSources
        }

        val list = arrayListOf<ViewItem>()

        TitleViewItem(
            id = "TITLE",
            text = translate["game_ipa_match_screen_title"].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnSurface)),
            textMargin = Margin(
                marginHorizontal = DP.DP_8
            )
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE_TOP", height = DP.DP_16))
            list.add(it)
        }


        val chooseList = choose.orEmpty().flatMap {

            listOf(it.first, it.second)
        }

        chooseList.mapIndexed { index, option ->

            val optionPair = chooseList.getOrNull(if (index % 2 == 0) index + 1 else index - 1)

            if (option == null) OptionViewItem(
                option = Option(Type.NONE, phonetic = Phonetic("$index")),
                background = Background(strokeColor = theme.colorPrimary, strokeDashEnable = true)
            )
            else OptionViewItem(
                option = option,
                newType = option.type,
                background = Background(strokeColor = if (warning.value == true && !optionPair?.phonetic?.text.equals(option.phonetic.text, true)) theme.colorOnErrorVariant else theme.colorPrimary)
            )
        }.let {

            list.add(SpaceViewItem(id = "SPACE_CHOOSE_TITLE", height = DP.DP_24))
            list.addAll(it)
        }

        quiz.match.flatMap {

            listOf(it.first, it.second)
        }.map {

            OptionViewItem(
                option = it,
                newType = if (it in chooseList) Type.NONE else it.type,

                background = Background(strokeColor = theme.colorOnSurfaceVariant, strokeDashEnable = true)
            )
        }.let {

            list.add(SpaceViewItem(id = "SPACE_OPTION_CHO0SE", height = DP.DP_70))
            list.addAll(it)
        }

        postDifferentValueIfActive(list)
    }

    val buttonInfo: LiveData<ButtonInfo> = listenerSources(theme, translate, choose) {

        val choose = choose.value

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val isClickable = !choose.orEmpty().flatMap { listOf<Any?>(it.first, it.second) }.any { it == null }

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
    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = MediatorLiveData()

    @VisibleForTesting
    val stateInfo: LiveData<StateInfo> = combineSources(theme, translate, consecutiveCorrectAnswerEvent) {

        val consecutiveCorrectAnswer = consecutiveCorrectAnswerEvent.value?.getContentIfNotHandled() ?: return@combineSources

        val theme = theme.get()
        val translate = translate.get()

        val isAnswerCorrect = consecutiveCorrectAnswer.first > 0

        val textColor = if (isAnswerCorrect) {
            theme.colorOnPrimaryVariant
        } else {
            theme.colorOnErrorVariant
        }

        val text = if (isAnswerCorrect) {
            translate["action_continue"]
        } else {
            translate["action_retry"]
        }.orEmpty()
            .with(ForegroundColorSpan(theme.colorOnPrimary))

        val title = if (isAnswerCorrect) {
            translate["title_answer_true"]
        } else {
            translate["title_answer_failed"]
        }.orEmpty()
            .with(ForegroundColorSpan(textColor))

        val message = if (isAnswerCorrect) {
            translate["game_ipa_match_screen_message_answer_correct"]
        } else {
            translate["game_ipa_match_screen_message_answer_failed"]
        }.orEmpty()
            .with(ForegroundColorSpan(textColor))

        val positive = ButtonInfo(
            text = text,
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


    fun resetChoose() {

        choose.postValue(choose.value.orEmpty().map { null to null })
    }

    fun updateChoose(it: Pair<Option?, Type?>) {

        val type = it.second ?: return
        val option = it.first ?: return

        if (type == Type.NONE) return

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

    fun updateListenerEnable(it: Boolean) {

        listenEnable.postDifferentValue(it)
    }

    fun updateResourceSelected(it: Word.Resource) {

        resourceSelected.postDifferentValue(it)
    }

    fun updateConsecutiveCorrectAnswer(event: Event<Pair<Long, Boolean>>) {

        consecutiveCorrectAnswerEvent.postDifferentValue(event)
    }

    fun startListen(data: Pair<Option, Type>, voiceId: Int, voiceSpeed: Float) = viewModelScope.launch(handler + Dispatchers.IO) {

        val map = listenState.value ?: return@launch

        map[data] = ResultState.Start
        listenState.postValue(map)

        val param = StartListenUseCase.Param(
            text = data.first.phonetic.text.orEmpty(),

            languageCode = inputLanguage.value?.id ?: Language.EN,

            voiceId = voiceId,
            voiceSpeed = voiceSpeed
        )

        var job: Job? = null

        job = startListenUseCase.execute(param).launchCollect(viewModelScope) { state ->

            map[data] = state
            listenState.postValue(map)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
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

    private fun getLoadingViewItem(theme: AppTheme): List<ViewItem> = arrayListOf<ViewItem>().apply {

        val background = Background(
            cornerRadius = DP.DP_24,
            backgroundColor = theme.colorLoading
        )

        add(IpaDetailLoadingViewItem(id = "1", background = background))

        add(SpaceViewItem(id = "2", height = DP.DP_24))

        addAll(getPhoneticLoadingViewItem(theme = theme))
    }

    private suspend fun OptionViewItem(option: Option? = null, newType: Type? = null, size: AppSize? = null, theme: AppTheme? = null, background: Background = Background()): ViewItem {

        val sizeWrap = size ?: this.size.asFlow().first()
        val themeWrap = theme ?: this.theme.asFlow().first()

        return if (newType == Type.VOICE) {
            OptionVoiceViewItem(option, newType, size = sizeWrap, theme = themeWrap, background = background)
        } else {
            OptionTextViewItem(option, newType, size = sizeWrap, theme = themeWrap, background = background)
        }
    }

    private fun OptionTextViewItem(option: Option?, newType: Type?, size: AppSize, theme: AppTheme, background: Background = Background()): ViewItem {

        val text = if (newType == Type.TEXT) {
            option?.phonetic?.text.orEmpty()
        } else if (newType == Type.IPA) {
            option?.phonetic?.ipa?.flatMap { it.value }?.firstOrNull().orEmpty()
        } else {
            ""
        }.with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface))

        return ClickTextViewItem(
            id = "${option?.phonetic?.text}_${option?.type?.name}_${newType?.name}",
            data = option to newType,

            text = text,
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
                width = (size.width - 2 * DP.DP_8) / 2,
                height = DP.DP_56
            ),
            padding = Padding(
                paddingVertical = DP.DP_8,
                paddingHorizontal = DP.DP_8
            )
        )
    }

    private fun OptionVoiceViewItem(option: Option?, newType: Type?, size: AppSize, theme: AppTheme, background: Background = Background()): ViewItem {

        val listenState = listenState.value.orEmpty()[option to newType]

        val image = if (listenState == null || listenState.isStart() || listenState.isCompleted()) {
            R.drawable.ic_volume_24dp
        } else {
            R.drawable.ic_pause_24dp
        }

        return ImageStateViewItem(
            id = "${option?.phonetic?.text}_${option?.type?.name}_${newType?.name}",
            data = option to newType,

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

            isLoading = listenState.isStart(),

            size = Size(
                width = (size.width - 2 * DP.DP_8) / 2,
                height = DP.DP_56
            ),
            padding = Padding(
                paddingVertical = DP.DP_8,
                paddingHorizontal = DP.DP_8
            )
        )
    }

    data class StateInfo(
        val anim: Int? = null,

        val title: CharSequence,
        val message: CharSequence,

        val background: Background = DEFAULT_BACKGROUND,

        val positive: com.simple.coreapp.utils.ext.ButtonInfo? = null,
    )

    data class ButtonInfo(
        val text: CharSequence,
        val isClickable: Boolean,

        val background: Background = DEFAULT_BACKGROUND
    )

    private class Quiz(
        val match: List<Pair<Option, Option>>,
    )

    data class Option(
        val type: Type,
        val phonetic: Phonetic
    )

    enum class Type {
        NONE, IPA, TEXT, VOICE
    }
}