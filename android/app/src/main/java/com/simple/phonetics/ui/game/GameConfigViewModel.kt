package com.simple.phonetics.ui.game

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.phonetics.word.entities.WordResourceCount
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RelativeSize
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.Constants
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.CountIpaAsyncUseCase
import com.simple.phonetics.domain.usecase.word.GetListWordResourceCountAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseActionViewModel
import com.simple.phonetics.utils.exts.OptionViewItem
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrEmpty
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.exts.removeSpecialCharacters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn

class GameConfigViewModel(
    private val countIpaAsyncUseCase: CountIpaAsyncUseCase,
    private val getListWordResourceCountAsyncUseCase: GetListWordResourceCountAsyncUseCase
) : BaseActionViewModel() {

    @VisibleForTesting
    val ipaCount: LiveData<Int> = combineSourcesWithDiff(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countIpaAsyncUseCase.execute(CountIpaAsyncUseCase.Param(languageCode = inputLanguage.id)).collect {

            postValue(it)
        }
    }


    @VisibleForTesting
    val wordCountMap: LiveData<List<WordResourceCount>> = combineSourcesWithDiff(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        getListWordResourceCountAsyncUseCase.execute(GetListWordResourceCountAsyncUseCase.Param(languageCode = inputLanguage.id)).collect {

            postValue(it)
        }
    }


    val resourceSelected: LiveData<String> = MediatorLiveData()


    val viewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(theme, translate, actionHeight, wordCountMap, resourceSelected) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val actionHeight = actionHeight.value ?: return@listenerSourcesWithDiff

        val resourceSelected = resourceSelected.value

        val list = arrayListOf<ViewItem>()

        TitleViewItem(
            id = "TITLE_RESOURCE",
            text = translate["game_config_screen_title_resource"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE_RESOURCE", height = DP.DP_24))
            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_TITLE_RESOURCE_1", height = DP.DP_12))
        }



        wordCountMap.getOrEmpty().mapNotNull {

            val count = it.count
            val resource = it.resource

            val id = (Id.RESOURCE + "_" + resource.removeSpecialCharacters()).lowercase()

            val name = if (translate.containsKey(id)) {
                translate[id].orEmpty()
            } else {
                return@mapNotNull null
            }

            val caption = if (count <= Constants.WORD_COUNT_MIN) {

                translate.getOrEmpty("game_config_screen_message_resource_limit")
            } else {

                translate.getOrEmpty("game_config_screen_message_resource_from_${resource.lowercase()}")
            }

            val isSelect = resource == resourceSelected

            val captionColor = if (count <= Constants.WORD_COUNT_MIN) {
                theme.getOrTransparent("colorOnErrorVariant")
            } else if (isSelect) {
                theme.getOrTransparent("colorOnPrimaryVariant")
            } else {
                theme.getOrTransparent("colorOnSurfaceVariant")
            }

            OptionViewItem(
                id = id,
                data = resource,

                text = "$name\n$caption"
                    .with(ForegroundColor(if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface")))
                    .with(caption, RelativeSize(0.8f), ForegroundColor(captionColor)),

                strokeColor = if (isSelect) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"),
                backgroundColor = if (isSelect) theme.getOrTransparent("colorPrimaryVariant") else Color.TRANSPARENT,
            )
        }.let {

            list.addAll(it)
            list.add(SpaceViewItem(id = "SPACE_TITLE_RESOURCE_3", height = actionHeight + DP.DP_24))
        }

        postValue(list)
    }

    val buttonInfo: LiveData<ClickTextViewItem> = listenerSourcesWithDiff(theme, translate, resourceSelected) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val resourceSelected = resourceSelected.value

        val isAvailable = resourceSelected != null

        ClickTextViewItem(
            id = "",
            text = translate["game_config_screen_action_play_game"].orEmpty()
                .with(ForegroundColor(if (isAvailable) theme.getOrTransparent("colorOnPrimary") else theme.getOrTransparent("colorOnSurface"))),
            textStyle = TextStyle(
                textSize = 18f,
                textGravity = Gravity.CENTER
            ),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT
            ),
            textPadding = Padding(
                paddingVertical = DP.DP_12,
                paddingHorizontal = DP.DP_16,
            ),
            textBackground = Background(
                cornerRadius = DP.DP_100,
                strokeWidth = DP.DP_1,
                strokeColor = if (isAvailable) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorOnSurface"),
                backgroundColor = if (isAvailable) theme.getOrTransparent("colorPrimary") else Color.TRANSPARENT
            ),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            )
        ).let {

            postValue(it)
        }
    }

    init {

        ipaCount.asFlow().launchIn(viewModelScope)
    }

    fun updateResource(resource: String) {

        if (wordCountMap.getOrEmpty().find { it.resource == resource }?.count.orZero() <= Constants.WORD_COUNT_MIN) {
            return
        }

        this.resourceSelected.postValue(resource)

        logAnalytics("game_config_resource_" + resource.removeSpecialCharacters().lowercase())
    }

    suspend fun getNextGame(): String {

        val translate = translate.asFlow().first()

        val listGameAvailable = arrayListOf<String>()

        if (translate.containsKey("game_ipa_match_screen_title")) {
            listGameAvailable.add(DeeplinkManager.GAME_IPA_MATCH)
        }

        listGameAvailable.add(DeeplinkManager.GAME_IPA_WORDLE)

        if (translate.containsKey("game_ipa_puzzle_screen_title") && ipaCount.asFlow().first().orZero() > 0) {
            listGameAvailable.add(DeeplinkManager.GAME_IPA_PUZZLE)
        }

        return listGameAvailable.random()
    }
}

