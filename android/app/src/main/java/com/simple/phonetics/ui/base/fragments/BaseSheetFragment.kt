package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.ext.setFullScreen

abstract class BaseSheetFragment<T : ViewBinding, VM : BaseViewModel>() : BaseViewModelSheetFragment<T, VM>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.window?.setFullScreen()

        super.onViewCreated(view, savedInstanceState)
    }
}