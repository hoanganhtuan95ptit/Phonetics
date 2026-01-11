package com.simple.phonetics.ui.base.services.transition.running.exts

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.simple.core.utils.extentions.toJson
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


fun FragmentActivity.getTransitionRunningInfo(): String {

    return viewModels<RunTransitionViewModel>().value.running.value.orEmpty().values
        .sortedBy { it.timeAdd }.sortedByDescending { it.isRunning }
        .map { " ${it.tag}-${it.isRunning}-${it.timeRunning} " }.toJson().let { "running:$it" }
}


fun FragmentActivity.endTransition(tag: String) {

    viewModels<RunTransitionViewModel>().value.endTransition(tag = tag)
}

fun FragmentActivity.startTransition(tag: String) {

    viewModels<RunTransitionViewModel>().value.startTransition(tag = tag)
}

suspend fun FragmentActivity.onTransitionRunningEndAwait() {

    viewModels<RunTransitionViewModel>().value.onTransitionRunningEndAwait()
}