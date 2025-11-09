package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.view.View
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.setFullScreen
import com.simple.phonetics.ui.base.services.transition.Transition

abstract class BaseFragment<T : androidx.viewbinding.ViewBinding, VM : BaseViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId), Transition {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.window?.setFullScreen()

        super.onViewCreated(view, savedInstanceState)
    }
}