package com.phonetics.word

import android.content.Context
import androidx.startup.Initializer
import com.phonetics.word.dao.WordProvider
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class WordInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                single {
                    WordProvider(get())
                }
            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
