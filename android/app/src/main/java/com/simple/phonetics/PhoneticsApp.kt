package com.simple.phonetics

import android.app.Application
import android.content.Context
import com.simple.phonetics.di.apiModule
import com.simple.phonetics.di.cacheModule
import com.simple.phonetics.di.daoModule
import com.simple.phonetics.di.repositoryModule
import com.simple.phonetics.di.taskModule
import com.simple.phonetics.di.useCaseModule
import com.simple.phonetics.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PhoneticsApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        share = this

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

    companion object {

        lateinit var share: Application
    }
}