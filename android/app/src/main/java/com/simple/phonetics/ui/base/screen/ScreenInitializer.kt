package com.simple.phonetics.ui.base.screen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.startup.Initializer
import com.hoanganhtuan95ptit.autobind.AutoBind
import com.hoanganhtuan95ptit.autobind.utils.exts.createObject
import com.simple.coreapp.utils.ext.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

class ScreenInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        if (context is Application) context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

                if (activity !is FragmentActivity) return

                activity.listenFragmentLifecycleCallbacks().launchIn(activity.lifecycleScope)

                AutoBind.loadNameAsync(activity.javaClass).launchCollect(activity.lifecycleScope) { list ->
                    list.map { it.createObject(ActivityService::class.java)?.setup(activity) }
                }

                AutoBind.loadAsync(ComponentService::class.java).launchCollect(activity.lifecycleScope) { list ->
                    list.map { it.setup(activity) }
                }

                AutoBind.loadAsync(ActivityService::class.java).launchCollect(activity.lifecycleScope) { list ->
                    list.map { it.setup(activity) }
                }
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun FragmentActivity.listenFragmentLifecycleCallbacks() = channelFlow<Unit> {

        val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {

            override fun onFragmentViewCreated(fm: FragmentManager, fragment: Fragment, v: View, savedInstanceState: Bundle?) {

                AutoBind.loadNameAsync(fragment.javaClass).launchCollect(fragment.viewLifecycleOwner.lifecycleScope) { list ->
                    list.map { it.createObject(FragmentService::class.java)?.setup(fragment) }
                }

                AutoBind.loadAsync(ComponentService::class.java).launchCollect(fragment.viewLifecycleOwner.lifecycleScope) { list ->
                    list.map { it.setup(fragment) }
                }

                AutoBind.loadAsync(FragmentService::class.java).launchCollect(fragment.viewLifecycleOwner.lifecycleScope) { list ->
                    list.map { it.setup(fragment) }
                }
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        awaitClose {
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        }
    }
}
