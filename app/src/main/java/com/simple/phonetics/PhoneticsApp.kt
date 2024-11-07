package com.simple.phonetics

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.coroutines.CoroutineContext

class PhoneticsApp : Application() {

    private val handler = CoroutineExceptionHandler { _: CoroutineContext, throwable: Throwable ->

        logCrashlytics("exception_captured", throwable)
    }

    private val syncUseCase: SyncUseCase by lazy {

        getKoin().get()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        startKoin {

            androidContext(this@PhoneticsApp)

            androidLogger(Level.NONE)

            modules(
                apiModule,
                daoModule,
                taskModule,
                cacheModule,
                useCaseModule,
                viewModelModule,
                repositoryModule,
            )
        }

    }


    override fun onCreate() {

        super.onCreate()

        with(ProcessLifecycleOwner.get()) {

            lifecycleScope.launch(handler + Dispatchers.IO) {

                syncUseCase.execute().launchIn(this)
            }
        }
    }
}