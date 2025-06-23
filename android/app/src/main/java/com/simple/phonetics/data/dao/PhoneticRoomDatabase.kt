package com.simple.phonetics.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObjectOrNull

object ListStringConverter {

    @JvmStatic
    @TypeConverter
    fun listStringToString(strings: List<String>): String = strings.joinToString { it }

    @JvmStatic
    @TypeConverter
    fun stringToListString(concatenatedStrings: String): List<String> = concatenatedStrings.split(",")
}

object HashMapConverter {

    @JvmStatic
    @TypeConverter
    fun hashMapToString(strings: HashMap<String, List<String>>): String = strings.toJson()

    @JvmStatic
    @TypeConverter
    fun stringToHashMap(concatenatedStrings: String): HashMap<String, List<String>> = concatenatedStrings.toObjectOrNull<HashMap<String, List<String>>>() ?: hashMapOf()
}

@Database(entities = [RoomHistory::class, RoomPhonetic::class, KeyTranslateRoom::class], version = 1, exportSchema = false)
@TypeConverters(ListStringConverter::class, HashMapConverter::class)
abstract class PhoneticRoomDatabase : RoomDatabase() {

    abstract fun providerHistoryDao(): HistoryDao

    abstract fun providerPhoneticDao(): PhoneticDao

    abstract fun providerKeyTranslateDao(): KeyTranslateDao
}

class PhoneticRoomDatabaseProvider(context: Context) {

    private val phoneticRoomDatabase by lazy {
        Room.databaseBuilder(context, PhoneticRoomDatabase::class.java, "phonetics_database")
            .build()
    }

    val historyDao by lazy {
        phoneticRoomDatabase.providerHistoryDao()
    }

    val phoneticDao by lazy {
        phoneticRoomDatabase.providerPhoneticDao()
    }

    @Deprecated("")
    val keyTranslateDao by lazy {
        phoneticRoomDatabase.providerKeyTranslateDao()
    }
}
