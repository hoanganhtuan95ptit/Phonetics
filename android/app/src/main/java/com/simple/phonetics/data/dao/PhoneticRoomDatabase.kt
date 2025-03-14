package com.simple.phonetics.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObjectOrNull
import com.simple.phonetics.data.dao.PhoneticRoomDatabaseConstants.VERSION

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

@Database(entities = [RoomHistory::class, RoomPhonetic::class, KeyTranslateRoom::class], version = VERSION, exportSchema = false)
@TypeConverters(ListStringConverter::class, HashMapConverter::class)
abstract class PhoneticRoomDatabase : RoomDatabase() {

    abstract fun providerHistoryDao(): HistoryDao

    abstract fun providerPhoneticDao(): PhoneticDao

    abstract fun providerKeyTranslateDao(): KeyTranslateDao
}

object PhoneticRoomDatabaseConstants {

    private val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(database: SupportSQLiteDatabase) {
            // Xóa bảng
            database.execSQL("DROP TABLE IF EXISTS key_translate")
        }
    }

    const val VERSION = 1

    fun instant(context: Context) = Room.databaseBuilder(context, PhoneticRoomDatabase::class.java, "phonetics_database")
//        .addMigrations(MIGRATION_1_2)
        .build()
}
