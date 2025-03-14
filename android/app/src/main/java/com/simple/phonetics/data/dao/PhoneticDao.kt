package com.simple.phonetics.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.simple.phonetics.entities.Phonetic

private const val TABLE_NAME = "phonetics"

@Dao
interface PhoneticDao {

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE IN (:textList)")
    fun getRoomListByTextList(textList: List<String>): List<RoomPhonetic>

    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()

    fun insertOrUpdateEntities(entities: List<Phonetic>) {

        entities.map {

            RoomPhonetic(ipa = it.ipa, text = it.text)
        }.let {

            insertOrUpdate(it)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(room: RoomPhonetic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomPhonetic>)
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
) {
    companion object {

        fun Phonetic.toRoom() = RoomPhonetic(
            text = text,
            ipa = ipa,
        )

        fun RoomPhonetic.toEntity() = Phonetic(
            text = text,
        ).apply {

            ipa = this@toEntity.ipa
        }
    }
}