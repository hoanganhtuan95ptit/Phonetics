package com.simple.phonetics.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

private const val TABLE_NAME = "phonetics"

@Dao
interface PhoneticsDao {

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE IN (:textList)")
    fun getRoomListByTextList(textList: List<String>): List<RoomPhonetics>


    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(room: RoomPhonetics)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomPhonetics>)
}

@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["text"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomPhonetics(
    var text: String = "",
    var ipa: HashMap<String, List<String>> = hashMapOf()
)