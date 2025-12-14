package com.simple.phonetics.ui.base.services.transition.running

import com.simple.phonetics.ui.base.fragments.BaseFragment

interface RunningTransitionService {

    var runTransitionViewModel: RunTransitionViewModel
    var runTransitionViewModelActivity: RunTransitionViewModel


    fun setupTransitionRunning(fragment: BaseFragment<*, *>)


    fun endTransition(tag: String)

    fun startTransition(tag: String)
}