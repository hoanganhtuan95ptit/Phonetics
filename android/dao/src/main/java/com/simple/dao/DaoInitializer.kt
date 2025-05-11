package com.simple.dao

import android.content.Context
import androidx.room.Room
import androidx.startup.Initializer
import com.simple.dao.ipa.IpaRoomDatabaseNew
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class DaoInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                single {
                    Room.databaseBuilder(get(), IpaRoomDatabaseNew::class.java, "ipa_database_new")
                        .build().providerIpaDao()
                }
            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
