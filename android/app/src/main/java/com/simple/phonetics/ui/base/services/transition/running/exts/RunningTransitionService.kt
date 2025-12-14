package com.simple.phonetics.ui.base.services.transition.running.exts

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.simple.phonetics.ui.base.services.transition.running.RunTransitionViewModel
import com.simple.phonetics.ui.base.services.transition.running.RunningTransitionService


fun Fragment.endTransition(tag: String) {

    if (this is RunningTransitionService) endTransition(tag = tag)
}

fun Fragment.startTransition(tag: String) {

    if (this is RunningTransitionService) startTransition(tag = tag)
}


suspend fun Fragment.onTransitionRunningEndAwait() {

    if (this is RunningTransitionService) runTransitionViewModel.onTransitionRunningEndAwait()
}

fun FragmentActivity.getTransitionRunning(): Map<String, Any> {

    return viewModels<RunTransitionViewModel>().value.running.value.orEmpty().filter { it.value.isRunning }
}

suspend fun FragmentActivity.onTransitionRunningEndAwait() {

    viewModels<RunTransitionViewModel>().value.onTransitionRunningEndAwait()
}