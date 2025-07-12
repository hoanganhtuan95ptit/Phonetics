package com.simple.phonetics.ui.game.items

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import java.util.UUID

abstract class GameItemViewModel : BaseViewModel() {

    val resourceSelected: LiveData<String> = MediatorLiveData(Word.Resource.Popular.value)

    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = MediatorLiveData()


    fun updateResourceSelected(it: String) {

        resourceSelected.postDifferentValue(it)
    }

    fun updateConsecutiveCorrectAnswer(event: Event<Pair<Long, Boolean>>) {

        consecutiveCorrectAnswerEvent.postDifferentValue(event)
    }


    data class StateInfo(
        val id: String = UUID.randomUUID().toString(),

        val positive: com.simple.coreapp.utils.ext.ButtonInfo? = null,

        val viewItemList: List<ViewItem> = emptyList(),

        val backgroundColor: Int = Color.TRANSPARENT,
    )

    data class ActionInfo(
        val text: RichText,
        val isClickable: Boolean,

        val background: Background = DEFAULT_BACKGROUND
    )
}