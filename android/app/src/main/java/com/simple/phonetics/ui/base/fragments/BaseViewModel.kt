package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.base.fragments.transition.TransitionViewModel
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.AppSize
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appInputLanguage
import com.simple.phonetics.utils.appOutputLanguage
import com.simple.phonetics.utils.appPhoneticCodeSelected
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

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        appInputLanguage.collect {

            postDifferentValue(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        appOutputLanguage.collect {

            postDifferentValue(it)
        }
    }

    val phoneticCodeSelected: LiveData<String> = mediatorLiveData {

        appPhoneticCodeSelected.collect {

            postDifferentValue(it)
        }
    }
}