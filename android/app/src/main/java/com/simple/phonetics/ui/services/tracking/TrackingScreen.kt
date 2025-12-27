package com.simple.phonetics.ui.services.tracking

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ActivityService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

@AutoBind(ActivityService::class)
class TrackingActivity : ActivityService {

    override fun setup(fragmentActivity: FragmentActivity) {

        logAnalytics("activity_open", "activity_name" to fragmentActivity.javaClass.simpleName.lowercase())

        fragmentActivity.fragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {


            override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
//                logAnalytics("fragment_attached", "fragment_name" to f.javaClass.simpleName.lowercase())
            }

            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
//                logAnalytics("fragment_detached", "fragment_name" to f.javaClass.simpleName.lowercase())
            }


            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
//                logAnalytics("fragment_created", "fragment_name" to f.javaClass.simpleName.lowercase())
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
//                logAnalytics("fragment_destroyed", "fragment_name" to f.javaClass.simpleName.lowercase())
            }


            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
//                logAnalytics("fragment_view_created", "fragment_name" to f.javaClass.simpleName.lowercase())
            }

            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
//                logAnalytics("fragment_view_destroyed", "fragment_name" to f.javaClass.simpleName.lowercase())

            }


            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                logAnalytics("fragment_resumed", "fragment_name" to f.javaClass.simpleName.lowercase())
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                logAnalytics("fragment_paused", "fragment_name" to f.javaClass.simpleName.lowercase())
            }
        })
    }


    private fun FragmentActivity.fragmentLifecycleCallbacks(fragmentLifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks) = channelFlow<Unit> {

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        awaitClose {

            supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        }
    }.launchIn(this.lifecycleScope)
}