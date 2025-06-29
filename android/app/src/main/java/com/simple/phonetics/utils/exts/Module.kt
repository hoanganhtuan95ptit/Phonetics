package com.simple.phonetics.utils.exts

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.PhoneticsApp
import com.simple.state.ResultState
import com.simple.state.doFailed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object ModuleSdk {

    private val splitInstallManager by lazy {
        SplitInstallManagerFactory.create(PhoneticsApp.share)
    }

    suspend fun downloadSync(moduleName: String) = channelFlow {

        if (splitInstallManager.installedModules.contains(moduleName)) {

            trySend(ResultState.Success(Unit))
            awaitClose()
            return@channelFlow
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        val listener = SplitInstallStateUpdatedListener { state ->

            when (state.status()) {

                SplitInstallSessionStatus.INSTALLED -> {

                    trySend(ResultState.Success(Unit))
                }

                SplitInstallSessionStatus.FAILED -> {

                    trySend(ResultState.Failed(RuntimeException("$moduleName:${state.errorCode()}")))
                }

                else -> {

                }
            }
        }

        splitInstallManager.registerListener(listener)

        splitInstallManager.startInstall(request).addOnFailureListener {

            trySend(ResultState.Failed(it))
        }

        awaitClose {
            splitInstallManager.unregisterListener(listener)
        }
    }.map {

        it.doFailed {
            logCrashlytics("download_module_sync_failed_$moduleName", it)
        }

        it
    }.first()
}