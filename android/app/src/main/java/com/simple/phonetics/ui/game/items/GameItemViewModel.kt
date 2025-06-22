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
import com.simple.phonetics.entities.Text
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel

abstract class GameItemViewModel : BaseViewModel() {

    val text: LiveData<Text> = MediatorLiveData(Text("", Text.Type.IPA))

    val resourceSelected: LiveData<Word.Resource> = MediatorLiveData(Word.Resource.Popular)

    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = MediatorLiveData()

    fun updateText(text: Text) {

        this.text.postDifferentValue(text)
    }

    fun updateResourceSelected(it: Word.Resource) {

        resourceSelected.postDifferentValue(it)
    }

    fun updateConsecutiveCorrectAnswer(event: Event<Pair<Long, Boolean>>) {

        consecutiveCorrectAnswerEvent.postDifferentValue(event)
    }


    data class StateInfo(
        val backgroundColor: Int = Color.TRANSPARENT,

        val viewItemList: List<ViewItem> = emptyList(),

        val positive: com.simple.coreapp.utils.ext.ButtonInfo? = null,
    )

    data class ActionInfo(
        val text: RichText,
        val isClickable: Boolean,

        val background: Background = DEFAULT_BACKGROUND
    )
}