package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.setFullScreen
import com.simple.phonetics.ui.base.services.transition.ConfigTransitionService
import com.simple.phonetics.ui.base.services.transition.ConfigTransitionServiceImpl
import com.simple.phonetics.ui.base.services.transition.LockTransitionService
import com.simple.phonetics.ui.base.services.transition.LockTransitionServiceImpl
import com.simple.phonetics.ui.base.services.transition.RunningTransitionService
import com.simple.phonetics.ui.base.services.transition.RunningTransitionServiceImpl
import com.simple.phonetics.utils.exts.listenerLayoutChangeAsync
import com.unknown.coroutines.launchCollect

abstract class BaseFragment<T : androidx.viewbinding.ViewBinding, VM : BaseViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId),
    LockTransitionService by LockTransitionServiceImpl(),
    ConfigTransitionService by ConfigTransitionServiceImpl(),
    RunningTransitionService by RunningTransitionServiceImpl() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupTransitionConfig(fragment = this)

        setupTransitionLock(fragment = this)
        setupTransitionRunning(fragment = this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupTransitionConfigOnViewCreated(this)

        setOnTransitionLockViewCreated(this)

        view.listenerLayoutChangeAsync().launchCollect(viewLifecycleOwner) {

            Log.d("tuanha", "onViewCreated: ${this.javaClass.name}")
        }
        activity?.window?.setFullScreen()

        super.onViewCreated(view, savedInstanceState)
    }
}