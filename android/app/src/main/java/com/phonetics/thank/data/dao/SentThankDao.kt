package com.phonetics.thank.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "send_thank"

@Dao
interface SendThankDao {

    fun getListAsync(): Flow<List<String>> = getRoomList().map { it.map { it.id } }

    @Query("SELECT * FROM $TABLE_NAME WHERE 1=1")
    fun getRoomList(): Flow<List<RoomSendThank>>

    fun insertOrUpdate(id: String) = insertOrUpdate(RoomSendThank(id = id))

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(id: RoomSendThank)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["id"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomSendThank(
    val id: String,
)