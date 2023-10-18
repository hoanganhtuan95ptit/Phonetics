package com.simple.phonetics

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.BaseApp
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.di.apiModule
import com.simple.phonetics.di.cacheModule
import com.simple.phonetics.di.daoModule
import com.simple.phonetics.di.repositoryModule
import com.simple.phonetics.di.taskModule
import com.simple.phonetics.di.useCaseModule
import com.simple.phonetics.di.viewModelModule
import com.simple.phonetics.domain.usecase.SyncUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.context.loadKoinModules
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.coroutines.CoroutineContext

class PhoneticsApp : BaseApp() {

    private val handler = CoroutineExceptionHandler { _: CoroutineContext, throwable: Throwable ->

        logCrashlytics(throwable)
    }

    private val syncUseCase: SyncUseCase by lazy {

        getKoin().get()
    }

    override fun onCreate() {

        loadKoinModules(
            listOf(
                apiModule,
                daoModule,
                taskModule,
                cacheModule,
                useCaseModule,
                viewModelModule,
                repositoryModule,
            )
        )

        super.onCreate()

        with(ProcessLifecycleOwner.get()) {

            lifecycleScope.launch(handler + Dispatchers.IO) {

                syncUseCase.execute().launchIn(this)
            }
        }
    }
}