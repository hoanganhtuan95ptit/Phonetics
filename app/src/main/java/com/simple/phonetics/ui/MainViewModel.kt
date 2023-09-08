package com.simple.phonetics.ui

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.base.viewmodels.BaseViewModel
import com.simple.coreapp.utils.extentions.liveData
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.translate.data.usecase.TranslateUseCase
import java.util.Locale

class MainViewModel(
    private val translateUseCase: TranslateUseCase
) : BaseViewModel() {

    val translateState: LiveData<ResultState<Boolean>> = liveData {

        postValue(ResultState.Start)

        translateUseCase.execute(TranslateUseCase.Param(listOf("hello"), "en", Locale.getDefault().language)).let {

            it.doSuccess {

                postValue(ResultState.Success(true))
            }

            it.doFailed {

                postValue(ResultState.Failed(it))
            }
        }
    }
}