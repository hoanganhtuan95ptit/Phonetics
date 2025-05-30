package com.simple.phonetics.data.dao

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow

private const val TABLE_NAME = "key_translate"

@Dao
@Deprecated("remove")
interface KeyTranslateDao {

    suspend fun insert(vararg entity: KeyTranslate) {

        insert(entity.map { KeyTranslateRoom(it.key, it.value, it.langCode) })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(languages: List<KeyTranslateRoom>)

    @Query("SELECT * FROM $TABLE_NAME WHERE langCode = :langCode")
    fun getAllAsync(langCode: String): Flow<List<KeyTranslateRoom>>


    @Query("SELECT COUNT(*) FROM $TABLE_NAME")
    fun count(): Int


    fun getAll(): List<KeyTranslate> = getRoomAll().map {
        KeyTranslate(
            key = it.key,
            value = it.value,
            langCode = it.langCode
        )
    }

    @Query("SELECT * FROM $TABLE_NAME")
    fun getRoomAll(): List<KeyTranslateRoom>
}

@Keep
@Deprecated("remove")
@Entity(tableName = TABLE_NAME, primaryKeys = ["key", "langCode"])
data class KeyTranslateRoom(
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "langCode") val langCode: String,
)