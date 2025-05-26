package com.simple.phonetics.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.simple.phonetics.data.dao.RoomPhonetic.Companion.toEntity
import com.simple.phonetics.data.dao.RoomPhonetic.Companion.toRoom
import com.simple.phonetics.entities.Phonetic

private const val TABLE_NAME = "phonetics"

@Dao
interface PhoneticDao {

    fun getListBy(textList: List<String>): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(textList = list.map { it.lowercase() }).groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE IN (:textList)")
    fun getRoomListBy(textList: List<String>): List<RoomPhonetic>


    fun getListBy(textList: List<String>, phoneticCode: String): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(textList = list.map { it.lowercase() }, "%\"$phoneticCode\"%").groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM PHONETICS WHERE text COLLATE NOCASE IN (:textList) AND UPPER(ipa) LIKE UPPER(:phoneticCode)")
    fun getRoomListBy(textList: List<String>, phoneticCode: String): List<RoomPhonetic>

    fun getListBy(ipa: String, textList: List<String>, phoneticCode: String): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(ipa = "%$ipa%", textList = list.map { it.lowercase() }, "%\"$phoneticCode\"%").groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM PHONETICS WHERE ipa COLLATE NOCASE LIKE :ipa AND text COLLATE NOCASE IN (:textList) AND ipa COLLATE NOCASE LIKE :phoneticCode")
    fun getRoomListBy(ipa: String, textList: List<String>, phoneticCode: String): List<RoomPhonetic>


    fun suggestPhonetics(text: String): List<Phonetic> = suggestRoomPhonetics(text = "$text%").groupBy {

        it.text.lowercase()
    }.mapValues {

        it.value.first()
    }.values.map {

        it.toEntity()
    }

    @Query("SELECT * FROM PHONETICS WHERE text LIKE :text LIMIT 10")
    fun suggestRoomPhonetics(text: String): List<RoomPhonetic>


    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()


    fun insertOrUpdateEntities(entities: List<Phonetic>) {

        entities.map {

            it.toRoom()
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