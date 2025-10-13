package com.simple.phonetics.ui

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.adapters.texts.TextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Id
import com.simple.phonetics.Id.TRANSLATE
import com.simple.phonetics.domain.usecase.phonetics.code.UpdatePhoneticCodeSelectedUseCase
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.voice.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.GetVoiceIdSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.UpdateVoiceIdSelectedUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.GetVoiceSpeedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.UpdateVoiceSpeedUseCase
import com.simple.phonetics.domain.usecase.translate.CheckSupportTranslateUseCase
import com.simple.phonetics.domain.usecase.translate.selected.GetTranslateSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.translate.selected.UpdateTranslateSelectedUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.config.adapters.VoiceSpeedViewItem
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isFailed
import com.simple.state.isStart
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.flow.first

class ConfigViewModel(
    private val checkSupportTranslateUseCase: CheckSupportTranslateUseCase,
    private val updateTranslateSelectedUseCase: UpdateTranslateSelectedUseCase,
    private val getTranslateSelectedAsyncUseCase: GetTranslateSelectedAsyncUseCase,

    private val getVoiceAsyncUseCase: GetVoiceAsyncUseCase,
    private val updateVoiceIdSelectedUseCase: UpdateVoiceIdSelectedUseCase,
    private val getVoiceIdSelectedAsyncUseCase: GetVoiceIdSelectedAsyncUseCase,

    private val updateVoiceSpeedUseCase: UpdateVoiceSpeedUseCase,
    private val getVoiceSpeedAsyncUseCase: GetVoiceSpeedAsyncUseCase,

    private val startReadingUseCase: StartReadingUseCase,

    private val updatePhoneticCodeSelectedUseCase: UpdatePhoneticCodeSelectedUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val phoneticViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff<List<ViewItem>>(theme, translate, inputLanguage, phoneticCodeSelected) {

        val theme = theme.get()
        val translate = translate.get()

        val language = inputLanguage.get()
        val phoneticCodeSelected = phoneticCodeSelected.get()

        language.listIpa.map {

            val key = "ipa_" + it.code.lowercase()

            val ipaName = if (translate.containsKey(key)) {
                translate[key] ?: it.name
            } else {
                it.name
            }

            val isSelect = it.code == phoneticCodeSelected

            createOptionViewItem(
                id = Id.IPA + "_" + it.code,
                data = it.code to isSelect,
                text = ipaName.with(ForegroundColor(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
                strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
                backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
            )
        }.let {

            postValue(it)
        }
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val isSupportTranslateState: LiveData<ResultState<Boolean>> = combineSourcesWithDiff(inputLanguage, outputLanguage) {


        val inputLanguageCode = inputLanguage.get().id
        val outputLanguageCode = outputLanguage.get().id

        val param = CheckSupportTranslateUseCase.Param(inputLanguageCode = inputLanguageCode, outputLanguageCode = outputLanguageCode)

        checkSupportTranslateUseCase.execute(param).collect { state ->

            postValueIfActive(state)

            logAnalytics("feature_translate_${state.javaClass.simpleName.lowercase()}")

            state.doSuccess {

                logAnalytics("feature_translate_${inputLanguageCode}_${outputLanguageCode}_${it}")
            }

            state.doFailed {

                logCrashlytics("feature_translate", it)
            }
        }
    }

    @VisibleForTesting
    val translateSelect: LiveData<String> = mediatorLiveData {

        postValue(getTranslateSelectedAsyncUseCase.execute().first())
    }

    val translateEnable: LiveData<Boolean> = combineSourcesWithDiff(isSupportTranslateState, translateSelect) {

        val translateState = isSupportTranslateState.get()
        val translateSelect = translateSelect.get()

        if (translateState !is ResultState.Success) {

            postValue(false)
            return@combineSourcesWithDiff
        }

        postValue(translateSelect.isNotBlank())
    }

    @VisibleForTesting
    val translateViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff<List<ViewItem>>(theme, translate, isSupportTranslateState, translateSelect) {

        val theme = theme.get()
        val translate = translate.get()

        val translateState = isSupportTranslateState.get()
        val translateSelect = translateSelect.get()

        if (translateState.isFailed()) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val id = if (translateState.isStart()) {
            ""
        } else {
            "0"
        }

        val text = if (id.isNotBlank()) {
            translate["message_support_translate"]
        } else {
            translate["message_translate_download"]
        }


        val list = arrayListOf<ViewItem>()

        val isSelect = translateSelect == id

        createOptionViewItem(
            id = "$TRANSLATE-$id",
            data = id to isSelect,
            text = text.orEmpty().with(ForegroundColor(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
            strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
            backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
        ).let {

            list.add(it)
        }

        postValue(list)
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val voiceState: LiveData<ResultState<List<Int>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getVoiceAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val listVoice: LiveData<List<Int>> = combineSourcesWithDiff(voiceState) {

        val state = voiceState.get()

        state.doSuccess {

            postValue(it)
        }

        state.doFailed {

            postValue(emptyList())
        }
    }

    @VisibleForTesting
    val voiceSpeed: LiveData<Float> = mediatorLiveData {

        postValue(getVoiceSpeedAsyncUseCase.execute().first())
    }

    @VisibleForTesting
    val voiceSpeedViewItemList: LiveData<List<VoiceSpeedViewItem>> = combineSourcesWithDiff(theme, translate, listVoice, voiceSpeed) {

        val translate = translate.get()

        val listVoice = listVoice.get()
        val voiceSpeed = voiceSpeed.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val list = arrayListOf<VoiceSpeedViewItem>()

        VoiceSpeedViewItem(
            start = 0f,
            end = 2f,

            text = translate["speed_lever"].orEmpty().replace("\$lever", "$voiceSpeed"),

            current = voiceSpeed
        ).let {

            list.add(it)
        }

        postValue(list)
    }

    @VisibleForTesting
    val voiceSelect: LiveData<Int> = mediatorLiveData {

        postValue(getVoiceIdSelectedAsyncUseCase.execute().first())
    }

    @VisibleForTesting
    val voiceViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, listVoice, voiceSelect, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val listVoice = listVoice.get()
        var voiceSelect = voiceSelect.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }

        // nếu voice không nằm trong danh sách thì lấy cái đầu tiên
        if (voiceSelect !in listVoice) {

            voiceSelect = listVoice.first()
        }

        listVoice.mapIndexed { index, voice ->

            val isSelect = voice == voiceSelect

            createOptionViewItem(
                id = "${Id.VOICE}-${voice}",
                data = voice to isSelect,
                text = translate["voice_index"].orEmpty()
                    .replace("\$index", "$index")
                    .with(ForegroundColor(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
                strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
                backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
            )
        }.let {

            postValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, phoneticViewItemList, voiceViewItemList, voiceSpeedViewItemList, translateViewItemList) {

        val theme = theme.value ?: return@combineSourcesWithDiff

        val list = arrayListOf<ViewItem>()

        val textColor = theme.colorOnSurface
        val strokeColor = theme.colorOnSurface
        val backgroundColor = Color.TRANSPARENT

        phoneticViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_PHONE_VIEW_ITEM",
                text = it.text.text.with(ForegroundColor(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        translateViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_TRANSLATE_VIEW_ITEM",
                text = it.text.text.with(ForegroundColor(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceSpeedViewItemList.getOrEmpty().firstOrNull()?.let {

            createOptionViewItem(
                id = "LIST_VOICE_SPEED_VIEW_ITEM",
                text = it.text.with(ForegroundColor(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_VOICE_VIEW_ITEM",
                text = it.text.text.with(ForegroundColor(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        postValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, phoneticViewItemList, voiceViewItemList, translateViewItemList, voiceSpeedViewItemList) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val list = arrayListOf<ViewItem>()


        phoneticViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_phonetic"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_PHONETIC", text = text.with(ForegroundColor(theme.colorOnSurface))))

            list.addAll(it)
        }

        translateViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_translate"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_TRANSLATE", text = text.with(ForegroundColor(theme.colorOnSurface))))

            list.addAll(it)
        }

        voiceSpeedViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = (translate.getOrEmpty("title_voice_speed") + " ${voiceSpeed.value}")
                .with(ForegroundColor(theme.colorOnSurface))
                .with("${voiceSpeed.value}", ForegroundColor(theme.colorPrimary))

            list.add(createTitleTextView(id = "TITLE_VOICE_SPEED", text = text))

            list.addAll(it)
        }

        voiceViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_VOICE", text = text.with(ForegroundColor(theme.colorOnSurface))))

            list.addAll(it)
        }

        postValue(list)
    }


    fun updateVoiceSpeed(current: Float) = launchWithTag("VOICE_SPEED") {

        this@ConfigViewModel.voiceSpeed.postValue(current)

        updateVoiceSpeedUseCase.execute(UpdateVoiceSpeedUseCase.Param(voiceSpeed = current))
    }

    fun updateVoiceSelect(voiceId: Int) = launchWithTag("VOICE_SELECT") {

        this@ConfigViewModel.voiceSelect.postValue(voiceId)

        updateVoiceIdSelectedUseCase.execute(UpdateVoiceIdSelectedUseCase.Param(voiceId = voiceId))


        val languageCode = inputLanguage.value?.id ?: return@launchWithTag

        val textTest = if (languageCode == Language.EN) {
            "hello"
        } else {
            return@launchWithTag
        }

        startReadingUseCase.execute(StartReadingUseCase.Param(textTest)).collect {

        }
    }

    fun updateTranslation(id: String) = launchWithTag("TRANSLATION") {

        val translate = if (translateSelect.value.isNullOrBlank()) id else ""

        this@ConfigViewModel.translateSelect.postValue(translate)

        updateTranslateSelectedUseCase.execute(UpdateTranslateSelectedUseCase.Param(translateSelected = translate))
    }

    fun updatePhoneticCodeSelect(phoneticCode: String) = launchWithTag("PHONETIC_CODE_SELECT") {

        this@ConfigViewModel.phoneticCodeSelected.postValue(phoneticCode)

        updatePhoneticCodeSelectedUseCase.execute(UpdatePhoneticCodeSelectedUseCase.Param(phoneticCode = phoneticCode))
    }

    private fun createTitleTextView(
        id: String,
        text: RichText
    ) = NoneTextViewItem(
        id = id,
        text = text.with(Bold),
        textStyle = TextStyle(
            textSize = 16f
        ),
        size = Size(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        ),
        padding = Padding(
            top = DP.DP_16,
            bottom = DP.DP_4
        )
    )

    private fun createOptionViewItem(
        id: String,

        text: RichText,

        data: Any? = null,

        strokeColor: Int,
        backgroundColor: Int
    ) = ClickTextViewItem(
        id = id,
        data = data,
        text = text,
        textStyle = TextStyle(
            textSize = 16f,
            textGravity = Gravity.CENTER
        ),
        padding = Padding(
            top = DP.DP_6,
            right = DP.DP_8,
            bottom = DP.DP_6
        ),
        textPadding = Padding(
            top = DP.DP_8,
            bottom = DP.DP_8,
            left = DP.DP_16,
            right = DP.DP_16
        ),
        textBackground = Background(
            cornerRadius = DP.DP_100,
            strokeWidth = DP.DP_1,
            strokeColor = strokeColor,
            backgroundColor = backgroundColor
        )
    )
}