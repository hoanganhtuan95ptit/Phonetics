package com.one.analytics.firebase

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.one.coreapp.App
import com.one.coreapp.data.task.analytics.Analytics
import com.one.coreapp.data.usecase.ResultState

class FirebaseAnalytics : Analytics {

    override suspend fun execute(param: Analytics.Param): ResultState<Unit> {

        FirebaseAnalytics.getInstance(App.shared).logEvent(param.name, bundleOf("data" to param.data))

        return ResultState.Success(Unit)
    }
}