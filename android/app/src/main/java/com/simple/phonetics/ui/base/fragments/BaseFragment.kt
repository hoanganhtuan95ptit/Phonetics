package com.simple.phonetics.ui.base.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.setFullScreen
import com.simple.phonetics.ui.base.services.transition.ConfigTransitionService
import com.simple.phonetics.ui.base.services.transition.ConfigTransitionServiceImpl
import com.simple.phonetics.ui.base.services.transition.LockTransitionService
import com.simple.phonetics.ui.base.services.transition.LockTransitionServiceImpl
import com.simple.phonetics.ui.base.services.transition.RunningTransitionService
import com.simple.phonetics.ui.base.services.transition.RunningTransitionServiceImpl
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class BaseFragment<T : androidx.viewbinding.ViewBinding, VM : BaseViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId),
    LifecycleService,
    ViewLifecycleService,
    LockTransitionService by LockTransitionServiceImpl(),
    ConfigTransitionService by ConfigTransitionServiceImpl(),
    RunningTransitionService by RunningTransitionServiceImpl() {

    override var stateFlow: MutableSharedFlow<LifecycleState> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

    override var lifecycleOwnerFlow: MutableSharedFlow<LifecycleOwner> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)


    override var viewStateFlow: MutableSharedFlow<LifecycleState> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

    override var viewLifecycleOwnerFlow = MutableSharedFlow<LifecycleOwner>(replay = 1, extraBufferCapacity = 1)


    override fun onAttach(context: Context) {

        stateFlow.tryEmit(LifecycleState.ATTACH)
        viewStateFlow.tryEmit(LifecycleState.ATTACH)

        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        stateFlow.tryEmit(LifecycleState.CREATED)
        lifecycleOwnerFlow.tryEmit(this)

        super.onCreate(savedInstanceState)

        setupTransitionConfig(fragment = this)

        setupTransitionLock(fragment = this)
        setupTransitionRunning(fragment = this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewStateFlow.tryEmit(LifecycleState.CREATED)
        viewLifecycleOwnerFlow.tryEmit(viewLifecycleOwner)

        activity?.window?.setFullScreen()

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {

        stateFlow.tryEmit(LifecycleState.RESUMED)
        viewStateFlow.tryEmit(LifecycleState.RESUMED)

        super.onResume()
    }

    override fun onPause() {

        stateFlow.tryEmit(LifecycleState.PAUSE)
        viewStateFlow.tryEmit(LifecycleState.PAUSE)

        super.onPause()
    }

    override fun onDestroyView() {

        viewStateFlow.tryEmit(LifecycleState.DESTROYED)

        super.onDestroyView()
    }

    override fun onDestroy() {

        stateFlow.tryEmit(LifecycleState.DESTROYED)

        super.onDestroy()
    }
}