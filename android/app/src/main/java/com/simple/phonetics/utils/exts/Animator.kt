package com.simple.phonetics.utils.exts

import android.animation.Animator
import androidx.core.animation.doOnEnd
import androidx.fragment.app.FragmentActivity
import com.simple.phonetics.ui.base.services.transition.running.exts.endTransition
import com.simple.phonetics.ui.base.services.transition.running.exts.startTransition
import java.util.UUID

fun Animator.startWithTransition(fragmentActivity: FragmentActivity, onEnd: () -> Unit) {

    val id = UUID.randomUUID().toString()
    fragmentActivity.startTransition(id)

    doOnEnd {
        fragmentActivity.endTransition(id)
        onEnd.invoke()
    }

    start()
}