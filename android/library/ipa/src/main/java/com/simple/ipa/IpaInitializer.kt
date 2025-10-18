package com.simple.ipa

import android.content.Context
import androidx.startup.Initializer
import com.simple.ipa.dao.IpaProviderV2
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class IpaInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                single {
                    IpaProviderV2(get())
                }
            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
