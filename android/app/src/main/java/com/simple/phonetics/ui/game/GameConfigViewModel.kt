package com.simple.phonetics.ui.game

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.Constants
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.CountIpaAsyncUseCase
import com.simple.phonetics.domain.usecase.word.CountWordAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.ButtonViewItem
import com.simple.phonetics.utils.exts.OptionViewItem
import com.simple.phonetics.utils.exts.TitleViewItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn

class GameConfigViewModel(
    private val countIpaAsyncUseCase: CountIpaAsyncUseCase,
    private val countWordAsyncUseCase: CountWordAsyncUseCase
) : BaseViewModel() {

    val ipaCount: LiveData<Int> = combineSources(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countIpaAsyncUseCase.execute(CountIpaAsyncUseCase.Param(languageCode = inputLanguage.id)).collect {

            postDifferentValue(it)
        }
    }

    val wordHistoryCount: LiveData<Int> = combineSources(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countWordAsyncUseCase.execute(CountWordAsyncUseCase.Param(resource = Word.Resource.History, languageCode = inputLanguage.id)).collect {

            postDifferentValue(it)
        }
    }

    val wordPopularCount: LiveData<Int> = combineSources(inputLanguage) {

        val inputLanguage = inputLanguage.get()

        countWordAsyncUseCase.execute(CountWordAsyncUseCase.Param(resource = Word.Resource.Popular, languageCode = inputLanguage.id)).collect {

            postDifferentValue(it)
        }
    }

    val resourceSelected: LiveData<Word.Resource> = MediatorLiveData()

    @VisibleForTesting
    val resourceViewItemList: LiveData<List<ViewItem>> = listenerSources(theme, translate, resourceSelected, wordHistoryCount, wordPopularCount) {

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val wordPopularCount = wordPopularCount.value ?: return@listenerSources
        val wordHistoryCount = wordHistoryCount.value ?: return@listenerSources

        val resourceSelected = resourceSelected.value

        val list = arrayListOf<ViewItem>()

        TitleViewItem(
            id = "TITLE_RESOURCE",
            text = translate["game_config_screen_title_resource"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
        ).let {

            list.add(SpaceViewItem(id = "SPACE_TITLE_RESOURCE", height = DP.DP_8))
            list.add(it)
            list.add(SpaceViewItem(id = "SPACE_TITLE_RESOURCE_1", height = DP.DP_8))
        }

        Word.Resource.entries.map {

            val count = when (it) {
                Word.Resource.Popular -> {
                    wordPopularCount
                }

                Word.Resource.History -> {
                    wordHistoryCount
                }
            }

            val name = translate[Id.RESOURCE.lowercase() + "_" + it.name.lowercase()].orEmpty()

            val caption = if (count <= Constants.WORD_COUNT_MIN) {

                translate["game_config_screen_message_resource_limit"].orEmpty()
            } else if (it == Word.Resource.Popular) {

                translate["game_config_screen_message_resource_from_popular"].orEmpty()
            } else if (it == Word.Resource.History) {

                translate["game_config_screen_message_resource_from_history"].orEmpty()
            } else {
                ""
            }

            val isSelect = it == resourceSelected

            val captionColor = if (count <= Constants.WORD_COUNT_MIN) {
                theme.colorOnErrorVariant
            } else if (isSelect) {
                theme.colorOnPrimaryVariant
            } else {
                theme.colorOnSurfaceVariant
            }

            OptionViewItem(
                id = Id.RESOURCE + "_" + it.name,
                data = it,

                text = "$name\n$caption"
                    .with(ForegroundColorSpan(if (isSelect) theme.colorPrimary else theme.colorOnSurface))
                    .with(caption, RelativeSizeSpan(0.8f), ForegroundColorSpan(captionColor)),

                strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
                backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT,
            )
        }.let {

            list.addAll(it)
        }

        postDifferentValue(list)
    }

    @VisibleForTesting
    val buttonViewItemList: LiveData<List<ViewItem>> = listenerSources(theme, translate, resourceSelected) {

        val theme = theme.value ?: return@listenerSources
        val translate = translate.value ?: return@listenerSources

        val resourceSelected = resourceSelected.value

        val list = arrayListOf<ViewItem>()

        val isAvailable = resourceSelected != null

        ButtonViewItem(
            id = Id.BUTTON,

            text = translate["game_config_screen_action_play_game"].orEmpty()
                .with(ForegroundColorSpan(if (isAvailable) theme.colorOnPrimary else theme.colorOnSurface)),

            strokeColor = if (isAvailable) theme.colorPrimary else theme.colorOnSurface,
            backgroundColor = if (isAvailable) theme.colorPrimary else Color.TRANSPARENT,
        ).let {

            list.add(SpaceViewItem(id = "SPACE_BUTTON", height = DP.DP_30))
            list.add(it)
        }

        postDifferentValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(resourceViewItemList, buttonViewItemList) {

        val list = arrayListOf<ViewItem>()

        list.addAll(resourceViewItemList.getOrEmpty())
        list.addAll(buttonViewItemList.getOrEmpty())

        postDifferentValue(list)
    }

    init {

        ipaCount.asFlow().launchIn(viewModelScope)
    }

    fun updateResource(resource: Word.Resource) {

        if (resource == Word.Resource.Popular && wordPopularCount.value.orZero() <= Constants.WORD_COUNT_MIN) {
            return
        } else if (resource == Word.Resource.History && wordHistoryCount.value.orZero() <= Constants.WORD_COUNT_MIN) {
            return
        }

        this.resourceSelected.postDifferentValue(resource)

        logAnalytics("game_config_resource_" + resource.value.lowercase())
    }

    suspend fun getNextGame(): String {

        val translate = translate.asFlow().first()

        val listGameAvailable = arrayListOf<String>()

        if (translate.containsKey("game_ipa_match_screen_title")) {
            listGameAvailable.add(Deeplink.GAME_IPA_MATCH)
        }

        listGameAvailable.add(Deeplink.GAME_IPA_WORDLE)

        if (translate.containsKey("game_ipa_puzzle_screen_title") && ipaCount.asFlow().first().orZero() > 0) {
            listGameAvailable.add(Deeplink.GAME_IPA_PUZZLE)
        }

        return listGameAvailable.random()
    }
}

