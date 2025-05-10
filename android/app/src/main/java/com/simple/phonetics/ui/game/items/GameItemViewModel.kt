package com.simple.phonetics.ui.game.items

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.domain.usecase.reading.CheckSupportReadingAsyncUseCase
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import org.koin.core.context.GlobalContext

abstract class GameItemViewModel : BaseViewModel() {

    val isSupportReading: LiveData<Boolean> = mediatorLiveData {

        GlobalContext.get().get<CheckSupportReadingAsyncUseCase>().execute().collect {

            postDifferentValue(it)
        }
    }

    val resourceSelected: LiveData<Word.Resource> = MediatorLiveData()

    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = MediatorLiveData()


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
        val text: CharSequence,
        val isClickable: Boolean,

        val background: Background = DEFAULT_BACKGROUND
    )
}