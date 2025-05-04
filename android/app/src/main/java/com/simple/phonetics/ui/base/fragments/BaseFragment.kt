package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.view.View
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.utils.ext.setFullScreen

abstract class BaseFragment<T : androidx.viewbinding.ViewBinding, VM : BaseViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : TransitionFragment<T, VM>(contentLayoutId) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.window?.setFullScreen()

        super.onViewCreated(view, savedInstanceState)
    }
}