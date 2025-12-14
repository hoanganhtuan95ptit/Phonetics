package com.simple.phonetics.ui.base.services.transition.locking.exts

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.simple.phonetics.ui.base.services.transition.locking.LockingTransitionService
import com.simple.phonetics.ui.base.services.transition.locking.LockingTransitionViewModel


fun Fragment.lockTransition(tag: String) {

    if (this is LockingTransitionService) lockTransition(tag = tag)
}

fun Fragment.unlockTransition(tag: String) {

    if (this is LockingTransitionService) unlockTransition(tag = tag)
}

fun FragmentActivity.getTransitionLock(): Map<String, LockingTransitionViewModel.Locking> {

    return viewModels<LockingTransitionViewModel>().value.locking.value.orEmpty().filter { it.value.isLocking }
}