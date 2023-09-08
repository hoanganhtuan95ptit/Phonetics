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
interface PhoneticsHistoryDao {

    @Query("SELECT * FROM $TABLE_NAME WHERE 1=1 ORDER BY timeCreate DESC")
    fun getRoomListByAsync(): Flow<List<RoomPhoneticHistory>>

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE == :text")
    fun getRoomListByTextAsync(text: String): List<RoomPhoneticHistory>


    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(room: RoomPhoneticHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomPhoneticHistory>)
}

@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["id"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomPhoneticHistory(
    var id: String = "",
    var text: String = "",
    var timeCreate: Long = System.currentTimeMillis()
)