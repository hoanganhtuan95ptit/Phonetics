package com.simple.phonetics.ui.game

import androidx.lifecycle.LiveData
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class GameViewModel : BaseViewModel() {

    val title: LiveData<CharSequence> = combineSources(theme, translate) {

        val theme = theme.get()
        val translate = translate.getOrEmpty()

        val title = translate["game_screen_title"].orEmpty()

        postDifferentValue(title)
    }
}