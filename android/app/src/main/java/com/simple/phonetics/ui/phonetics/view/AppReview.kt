package com.simple.phonetics.ui.phonetics.view

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.google.android.play.core.review.ReviewManagerFactory
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull

interface AppReview {

    fun showReview()

    fun setupAppView(detectFragment: PhoneticsFragment)
}

class AppReviewImpl : AppReview {

    private val openReview: MediatorLiveData<Event<Long>> = MediatorLiveData()

    override fun showReview() {

        openReview.postDifferentValue(System.currentTimeMillis().toEvent())
    }

    override fun setupAppView(detectFragment: PhoneticsFragment) {

        openReview.asFlow().launchCollect(detectFragment.viewLifecycleOwner) { event ->

            event.getContentIfNotHandled() ?: return@launchCollect

            openReview(detectFragment)
        }
    }


    private suspend fun openReview(fragment: PhoneticsFragment) {

        val manager = ReviewManagerFactory.create(fragment.context ?: return)

        val reviewInfo = channelFlow {

            manager.requestReviewFlow().addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    trySend(task.result)
                    logAnalytics("app_review_request_success")
                } else {

                    trySend(null)
                    logCrashlytics("app_review_request_failed", task.exception ?: RuntimeException("not found error"))
                }
            }

            awaitClose {

            }
        }.firstOrNull()

        channelFlow {

            val flow = manager.launchReviewFlow(fragment.activity ?: return@channelFlow, reviewInfo ?: return@channelFlow)

            flow.addOnCompleteListener { it ->

                trySend(Unit)

                if (it.isSuccessful) {

                    logAnalytics("app_review_open_success")
                } else {

                    logCrashlytics("app_review_open_failed", it.exception ?: RuntimeException("not found error"))
                }
            }

            awaitClose {

            }
        }.firstOrNull()
    }
}