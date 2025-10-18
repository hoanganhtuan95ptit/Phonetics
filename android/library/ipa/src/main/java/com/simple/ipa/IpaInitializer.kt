package com.simple.ipa

import android.content.Context
import androidx.startup.Initializer
import com.simple.ipa.dao.IpaProvider
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class IpaInitializer : Initializer<Unit> {

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
