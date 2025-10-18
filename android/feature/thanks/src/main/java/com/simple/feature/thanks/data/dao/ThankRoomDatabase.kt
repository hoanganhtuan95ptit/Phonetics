package com.simple.feature.thanks.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.context.GlobalContext

@Database(entities = [RoomSendThank::class, RoomPendingThank::class], version = 1, exportSchema = false)
abstract class ThankRoomDatabase : RoomDatabase() {

    abstract fun providerSendThankDao(): SendThankDao

    abstract fun providerPendingThankDao(): PendingThankDao
}

object ThankProvider {

    val room by lazy {
        Room.databaseBuilder(GlobalContext.get().get<Context>(), ThankRoomDatabase::class.java, "thank_database_v3")
            .build()
    }

    val sendThankDao by lazy {
        room.providerSendThankDao()
    }

    val pendingThankDao by lazy {
        room.providerPendingThankDao()
    }
}