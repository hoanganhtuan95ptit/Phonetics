package com.simple.phonetics.ui.game.items

import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.phonetics.ui.base.fragments.BaseViewModel

abstract class GameItemViewModel : BaseViewModel() {

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
}