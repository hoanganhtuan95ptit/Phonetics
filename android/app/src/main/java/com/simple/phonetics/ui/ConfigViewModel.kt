package com.simple.phonetics.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.adapters.texts.TextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.Id
import com.simple.phonetics.Id.TRANSLATE
import com.simple.phonetics.domain.usecase.phonetics.code.UpdatePhoneticCodeSelectedUseCase
import com.simple.phonetics.domain.usecase.reading.voice.GetVoiceAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.GetVoiceIdSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.selected.UpdateVoiceIdSelectedUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.GetVoiceSpeedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.voice.speed.UpdateVoiceSpeedUseCase
import com.simple.phonetics.domain.usecase.translate.TranslateUseCase
import com.simple.phonetics.domain.usecase.translate.selected.GetTranslateSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.translate.selected.UpdateTranslateSelectedUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.config.adapters.VoiceSpeedViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isFailed
import com.simple.state.isStart
import com.simple.state.toSuccess
import kotlinx.coroutines.flow.first

class ConfigViewModel(
    private val translateUseCase: TranslateUseCase,
    private val updateTranslateSelectedUseCase: UpdateTranslateSelectedUseCase,
    private val getTranslateSelectedAsyncUseCase: GetTranslateSelectedAsyncUseCase,

    private val getVoiceAsyncUseCase: GetVoiceAsyncUseCase,
    private val updateVoiceIdSelectedUseCase: UpdateVoiceIdSelectedUseCase,
    private val getVoiceIdSelectedAsyncUseCase: GetVoiceIdSelectedAsyncUseCase,

    private val updateVoiceSpeedUseCase: UpdateVoiceSpeedUseCase,
    private val getVoiceSpeedAsyncUseCase: GetVoiceSpeedAsyncUseCase,

    private val updatePhoneticCodeSelectedUseCase: UpdatePhoneticCodeSelectedUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val phoneticViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(theme, translate, inputLanguage, phoneticCodeSelected) {

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
                text = ipaName.with(ForegroundColorSpan(if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"))),
                strokeColor = if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"),
                backgroundColor = if (isSelect) theme.getOrTransparent("colorPrimaryVariant") else Color.TRANSPARENT
            )
        }.let {

            postDifferentValue(it)
        }
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val translateState: LiveData<ResultState<Boolean>> = combineSources(inputLanguage, outputLanguage) {

        postValue(ResultState.Start)

        val inputLanguageCode = inputLanguage.get().id
        val outputLanguageCode = outputLanguage.get().id

        translateUseCase.execute(TranslateUseCase.Param(listOf("hello"), inputLanguageCode, outputLanguageCode)).let { state ->

            state.toSuccess()?.data?.firstOrNull()?.translateState?.doSuccess {

                postValue(ResultState.Success(true))
            }

            state.doFailed {

                postValue(ResultState.Failed(it))
            }
        }
    }

    @VisibleForTesting
    val translateSelect: LiveData<String> = mediatorLiveData {

        postDifferentValue(getTranslateSelectedAsyncUseCase.execute().first())
    }

    val translateEnable: LiveData<Boolean> = combineSources(translateState, translateSelect) {

        val translateState = translateState.get()
        val translateSelect = translateSelect.get()

        if (translateState.isFailed()) {

            postDifferentValue(false)
            return@combineSources
        }

        if (translateState !is ResultState.Success) {

            return@combineSources
        }

        postDifferentValue(translateSelect.isNotBlank())
    }

    @VisibleForTesting
    val translateViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(theme, translate, translateState, translateSelect) {

        val theme = theme.get()
        val translate = translate.get()

        val translateState = translateState.get()
        val translateSelect = translateSelect.get()

        if (translateState.isFailed()) {

            postDifferentValue(emptyList())
            return@combineSources
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
            text = text.orEmpty().with(ForegroundColorSpan(if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"))),
            strokeColor = if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"),
            backgroundColor = if (isSelect) theme.getOrTransparent("colorPrimaryVariant") else Color.TRANSPARENT
        ).let {

            list.add(it)
        }

        postDifferentValue(list)
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
    val listVoice: LiveData<List<Int>> = combineSources(voiceState) {

        val state = voiceState.get()

        state.doSuccess {

            postDifferentValue(it)
        }

        state.doFailed {

            postDifferentValue(emptyList())
        }
    }

    @VisibleForTesting
    val voiceSpeed: LiveData<Float> = mediatorLiveData {

        postDifferentValue(getVoiceSpeedAsyncUseCase.execute().first())
    }

    @VisibleForTesting
    val voiceSpeedViewItemList: LiveData<List<VoiceSpeedViewItem>> = combineSources(theme, translate, listVoice, voiceSpeed) {

        val translate = translate.get()

        val listVoice = listVoice.get()
        val voiceSpeed = voiceSpeed.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
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

        postDifferentValue(list)
    }

    @VisibleForTesting
    val voiceSelect: LiveData<Int> = mediatorLiveData {

        postDifferentValue(getVoiceIdSelectedAsyncUseCase.execute().first())
    }

    @VisibleForTesting
    val voiceViewItemList: LiveData<List<ViewItem>> = combineSources(theme, listVoice, voiceSelect, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val listVoice = listVoice.get()
        var voiceSelect = voiceSelect.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
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
                    .with(ForegroundColorSpan(if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"))),
                strokeColor = if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"),
                backgroundColor = if (isSelect) theme.getOrTransparent("colorPrimaryVariant") else Color.TRANSPARENT
            )
        }.let {

            postDifferentValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItem>> = combineSources(theme, phoneticViewItemList, voiceViewItemList, voiceSpeedViewItemList, translateViewItemList) {

        val theme = theme.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()

        val textColor = theme.getOrTransparent("colorOnSurface")
        val strokeColor = theme.getOrTransparent("colorOnSurface")
        val backgroundColor = Color.TRANSPARENT

        phoneticViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_PHONE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        translateViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_TRANSLATE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceSpeedViewItemList.getOrEmpty().firstOrNull()?.let {

            createOptionViewItem(
                id = "LIST_VOICE_SPEED_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_VOICE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        postDifferentValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, phoneticViewItemList, voiceViewItemList, translateViewItemList, voiceSpeedViewItemList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()


        phoneticViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_phonetic"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_PHONETIC", text = text.with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))))

            list.addAll(it)
        }

        translateViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_translate"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_TRANSLATE", text = text.with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))))

            list.addAll(it)
        }

        voiceSpeedViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice_speed"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_VOICE_SPEED", text = text.with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))))

            list.addAll(it)
        }

        voiceViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice"].orEmpty()

            list.add(createTitleTextView(id = "TITLE_VOICE", text = text.with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))))

            list.addAll(it)
        }

        postDifferentValue(list)
    }


    fun updateVoiceSpeed(current: Float) = launchWithTag("VOICE_SPEED") {

        this@ConfigViewModel.voiceSpeed.postDifferentValue(current)

        updateVoiceSpeedUseCase.execute(UpdateVoiceSpeedUseCase.Param(voiceSpeed = current))
    }

    fun updateVoiceSelect(voiceId: Int) = launchWithTag("VOICE_SELECT") {

        this@ConfigViewModel.voiceSelect.postDifferentValue(voiceId)

        updateVoiceIdSelectedUseCase.execute(UpdateVoiceIdSelectedUseCase.Param(voiceId = voiceId))
    }

    fun updateTranslation(id: String) = launchWithTag("TRANSLATION") {

        val translate = if (translateSelect.value.isNullOrBlank()) id else ""

        this@ConfigViewModel.translateSelect.postDifferentValue(translate)

        updateTranslateSelectedUseCase.execute(UpdateTranslateSelectedUseCase.Param(translateSelected = translate))
    }

    fun updatePhoneticCodeSelect(phoneticCode: String) = launchWithTag("PHONETIC_CODE_SELECT") {

        this@ConfigViewModel.phoneticCodeSelected.postDifferentValue(phoneticCode)

        updatePhoneticCodeSelectedUseCase.execute(UpdatePhoneticCodeSelectedUseCase.Param(phoneticCode = phoneticCode))
    }

    private fun createTitleTextView(
        id: String,
        text: CharSequence
    ) = NoneTextViewItem(
        id = id,
        text = text.with(StyleSpan(Typeface.BOLD)),
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

        text: CharSequence,

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

