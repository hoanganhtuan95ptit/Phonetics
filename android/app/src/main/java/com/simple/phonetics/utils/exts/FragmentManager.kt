package com.simple.phonetics.utils.exts

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.simple.phonetics.R

fun FragmentActivity.replace(fragment: Fragment, containerViewId: Int = R.id.fragment_container, extras: Map<String, Any?>?, sharedElement: Map<String, View>?) {

    supportFragmentManager.replace(fragment = fragment, containerViewId = containerViewId, extras = extras, sharedElement = sharedElement)
}

fun Fragment.replace(fragment: Fragment, containerViewId: Int = R.id.fragment_container, extras: Map<String, Any?>?, sharedElement: Map<String, View>?) {

    childFragmentManager.replace(fragment = fragment, containerViewId = containerViewId, extras = extras, sharedElement = sharedElement)
}

fun FragmentManager.replace(fragment: Fragment, containerViewId: Int = R.id.fragment_container, extras: Map<String, Any?>?, sharedElement: Map<String, View>?) {

    fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())

    val fragmentTransaction = beginTransaction()

    sharedElement?.forEach { (t, u) ->

        fragmentTransaction.addSharedElement(u, t)
    }

    fragmentTransaction
        .replace(containerViewId, fragment, "")
        .addToBackStack("")
        .commitAllowingStateLoss()
}