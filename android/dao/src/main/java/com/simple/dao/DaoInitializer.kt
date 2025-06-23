package com.simple.dao

import android.content.Context
import androidx.startup.Initializer
import com.simple.dao.ipa.IpaProvider
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class DaoInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                single {
                    IpaProvider(get())
                }
            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
