package com.simple.phonetics.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

private const val TABLE_NAME = "phonetics"

@Dao
@Deprecated("")
interface PhoneticDao {


    @Query("SELECT COUNT(DISTINCT text) FROM $TABLE_NAME WHERE ipa IS NOT NULL AND ipa != '{}' AND ipa != ''")
    fun countByText(): Int


    @Query("SELECT * FROM $TABLE_NAME LIMIT :limit OFFSET :offset")
    fun allTextLimit(limit: Int, offset: Int): List<RoomPhonetic>
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["text"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomPhonetic(
    var text: String = "",
    var ipa: HashMap<String, List<String>> = hashMapOf()
)