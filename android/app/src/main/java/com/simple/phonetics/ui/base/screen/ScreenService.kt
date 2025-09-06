@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.simple.phonetics.ui.base.screen

import android.app.Activity
import android.content.ComponentCallbacks
import androidx.fragment.app.Fragment

interface ScreenService<T> {

    fun setup(t: T)
}

interface ActivityService : ScreenService<Activity> {

    override fun setup(activity: Activity) {
    }
}

interface FragmentService : ScreenService<Fragment> {

    override fun setup(fragment: Fragment) {
    }
}

interface ComponentService : ScreenService<ComponentCallbacks> {

    override fun setup(component: ComponentCallbacks) {
    }
}