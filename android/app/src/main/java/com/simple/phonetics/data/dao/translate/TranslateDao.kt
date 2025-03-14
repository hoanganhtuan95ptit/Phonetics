package com.simple.phonetics.data.dao.translate

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "translates"

@Dao
interface TranslateDao {

    fun getAllAsync(languageCode: String): Flow<Map<String, String>> = getRoomAllAsync(languageCode = languageCode).map { list ->

        list.map {
            it.key to it.value
        }.toMap()
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE languageCode = :languageCode")
    fun getRoomAllAsync(languageCode: String): Flow<List<RoomTranslate>>

    fun insertOrUpdate(languageCode: String, map: Map<String, String>) {

        val list = map.map {
            RoomTranslate(key = it.key, value = it.value, languageCode = languageCode)
        }

        insertOrUpdate(list)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(list: List<RoomTranslate>)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["key", "languageCode"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomTranslate(
    val key: String,
    val value: String,
    val languageCode: String,
)

@Database(entities = [RoomTranslate::class], version = 1, exportSchema = false)
abstract class TranslateRoomDatabase : RoomDatabase() {

    abstract fun providerTranslateDao(): TranslateDao
}
