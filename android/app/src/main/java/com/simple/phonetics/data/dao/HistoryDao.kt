package com.simple.phonetics.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.flow.Flow

private const val TABLE_NAME = "phonetic_history"

@Dao
interface HistoryDao {

    @Query("SELECT * FROM $TABLE_NAME WHERE 1=1 ORDER BY timeCreate DESC")
    fun getRoomListByAsync(): Flow<List<RoomHistory>>

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE == :text")
    fun getRoomListByTextAsync(text: String): List<RoomHistory>


    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(room: RoomHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomHistory>)
}

@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["id"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomHistory(
    var id: String = "",
    var text: String = "",
    var timeCreate: Long = System.currentTimeMillis()
)