package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.base.fragments.transition.TransitionViewModel
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.utils.AppSize
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appSize
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.appTranslate

abstract class BaseViewModel : TransitionViewModel() {

    val size: LiveData<AppSize> = mediatorLiveData {

        appSize.collect {

            postDifferentValue(it)
        }
    }

    val theme: LiveData<AppTheme> = mediatorLiveData {

        appTheme.collect {

            postDifferentValue(it)
        }
    }

    val translate: LiveData<Map<String, String>> = mediatorLiveData {

        appTranslate.collect {

            postDifferentValue(it)
        }
    }
}