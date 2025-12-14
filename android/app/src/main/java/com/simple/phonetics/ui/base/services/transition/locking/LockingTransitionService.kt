package com.simple.phonetics.ui.base.services.transition.locking

import com.simple.phonetics.ui.base.fragments.BaseFragment

interface LockingTransitionService {

    var lockingTransitionViewModel: LockingTransitionViewModel
    var lockingTransitionViewModelActivity: LockingTransitionViewModel


    fun setupTransitionLock(fragment: BaseFragment<*, *>)


    fun lockTransition(tag: String)

    fun unlockTransition(tag: String)
}
