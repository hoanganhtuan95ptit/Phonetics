package com.simple.phonetics.ui.game_ipa_wordle

import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.ui.base.CommonViewModel

class GameIPAWordleViewModel : CommonViewModel() {

    val ipaState: LiveData<List<Phonetics>> = mediatorLiveData {

    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, ipaState) {

        val size = size.get()
        val theme = theme.get()
        val translate = translate.get()

        val list = arrayListOf<ViewItem>()

        

        postDifferentValueIfActive(list)
    }
}