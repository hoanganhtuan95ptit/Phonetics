package com.simple.phonetics.ui.services.queue

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.event.sendEvent
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.view.HomeScreen
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

@AutoBind(MainActivity::class)
class QueueEventStateService : MainService {

    override fun setup(mainActivity: MainActivity) {

        mainActivity.listenerActivityLifecycleCallbacks(object : DefaultActivityLifecycleCallbacks() {

            override fun onActivityResumed(activity: Activity) {
                QueueEventState.updateState("main", ResultState.Success(Unit))
            }

            override fun onActivityPaused(activity: Activity) {
                QueueEventState.updateState("main", ResultState.Start)
            }
        })

        mainActivity.listenerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {

            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                if (f is HomeScreen) QueueEventState.updateState("home", ResultState.Success(Unit))
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                if (f is HomeScreen) QueueEventState.updateState("home", ResultState.Start)
            }
        })

        QueueEventState.getQueueAsync().launchCollect(mainActivity) { eventName ->

            sendEvent(eventName = eventName, data = Unit)
        }
    }

    private fun FragmentActivity.listenerFragmentLifecycleCallbacks(listener: FragmentManager.FragmentLifecycleCallbacks) = channelFlow<Unit> {

        supportFragmentManager.registerFragmentLifecycleCallbacks(listener, true)

        awaitClose {

            supportFragmentManager.unregisterFragmentLifecycleCallbacks(listener)
        }
    }.launchIn(lifecycleScope)


    private fun FragmentActivity.listenerActivityLifecycleCallbacks(listener: Application.ActivityLifecycleCallbacks) = channelFlow<Unit> {

        application.registerActivityLifecycleCallbacks(listener)

        awaitClose {

            application.unregisterActivityLifecycleCallbacks(listener)
        }
    }.launchIn(lifecycleScope)


    private abstract class DefaultActivityLifecycleCallbacks: Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
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
    }
}