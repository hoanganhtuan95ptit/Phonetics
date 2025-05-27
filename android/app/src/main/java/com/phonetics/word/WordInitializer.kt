package com.phonetics.word

import android.content.Context
import androidx.room.Room
import androidx.startup.Initializer
import com.phonetics.word.dao.WordRoomDatabaseV2
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class WordInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                single {
                    Room.databaseBuilder(get(), WordRoomDatabaseV2::class.java, "word_database_v2")
                        .build().providerWordDao()
                }

            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
