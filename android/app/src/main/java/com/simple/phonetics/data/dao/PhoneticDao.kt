package com.simple.phonetics.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.simple.phonetic.entities.Phonetic

private const val TABLE_NAME = "phonetics"

@Dao
@Deprecated("")
interface PhoneticDao {

    fun getListBy(textList: List<String>): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(textList = list.map { it.lowercase() }).groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.flatMap {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE text COLLATE NOCASE IN (:textList)")
    fun getRoomListBy(textList: List<String>): List<RoomPhonetic>


    fun getListBy(textList: List<String>, phoneticCode: String): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(textList = list, "%\"$phoneticCode\"%").groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.flatMap {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM PHONETICS WHERE text COLLATE NOCASE IN (:textList) AND UPPER(ipa) LIKE UPPER(:phoneticCode)")
    fun getRoomListBy(textList: List<String>, phoneticCode: String): List<RoomPhonetic>

    fun getListBy(ipa: String, textList: List<String>, phoneticCode: String): List<Phonetic> = textList.chunked(300).flatMap { list ->

        getRoomListBy(ipa = "%$ipa%", textList = list, "%\"$phoneticCode\"%").groupBy {

            it.text.lowercase()
        }.mapValues {

            it.value.first()
        }.values.flatMap {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM PHONETICS WHERE ipa COLLATE NOCASE LIKE :ipa AND text COLLATE NOCASE IN (:textList) AND ipa COLLATE NOCASE LIKE :phoneticCode")
    fun getRoomListBy(ipa: String, textList: List<String>, phoneticCode: String): List<RoomPhonetic>


    fun suggestPhonetics(text: String): List<Phonetic> = suggestRoomPhonetics(text = "$text%").groupBy {

        it.text.lowercase()
    }.mapValues {

        it.value.first()
    }.values.flatMap {

        it.toEntity()
    }

    @Query("SELECT * FROM PHONETICS WHERE text LIKE :text LIMIT 10")
    fun suggestRoomPhonetics(text: String): List<RoomPhonetic>


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

fun RoomPhonetic.toEntity() = ipa.map {
    Phonetic(
        textWrap = text.lowercase(),
        ipaCodeWrap = it.key.lowercase(),
        ipaValueList = it.value
    )
}