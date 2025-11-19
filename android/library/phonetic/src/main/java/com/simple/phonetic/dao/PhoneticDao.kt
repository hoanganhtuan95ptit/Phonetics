package com.simple.phonetic.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simple.core.utils.extentions.toJson
import com.simple.phonetic.PhoneticInitializer
import com.simple.phonetic.entities.Phonetic

private const val TABLE_NAME = "phonetics"
private const val TABLE_NAME_FTS = "${TABLE_NAME}_fts"

@Dao
interface PhoneticDaoV2 {

    fun getListBy(textList: List<String>, limit: Int = Int.MAX_VALUE) = textList.chunked(300).flatMap {

        getListRoomBy(textList = it, limit = limit)
    }

    @Query("SELECT r.* FROM $TABLE_NAME_FTS f JOIN $TABLE_NAME r ON f.rowid = r.rowid WHERE r.text IN (:textList) LIMIT :limit")
    fun getListRoomBy(textList: List<String>, limit: Int = Int.MAX_VALUE): List<Phonetic>


    fun getListBy(ipaCode: String, textList: List<String>, limit: Int = Int.MAX_VALUE) = textList.chunked(300).flatMap {

        getListRoomBy(ipaCode = ipaCode, textList = it, limit = limit)
    }

    @Query("SELECT r.* FROM $TABLE_NAME_FTS f JOIN $TABLE_NAME r ON f.rowid = r.rowid WHERE r.ipaCode = :ipaCode AND r.text IN (:textList) LIMIT :limit")
    fun getListRoomBy(ipaCode: String, textList: List<String>, limit: Int = Int.MAX_VALUE): List<Phonetic>


    fun getListBy(ipaQuery: String, ipaCode: String, textList: List<String>, limit: Int = Int.MAX_VALUE) = textList.chunked(300).flatMap {

        getListRoomBy(ipaQuery = ipaQuery, ipaCode = ipaCode, textList = it, limit = limit)
    }

    @Query("SELECT r.* FROM $TABLE_NAME_FTS f JOIN $TABLE_NAME r ON f.rowid = r.rowid WHERE f.ipaValue LIKE '%' || :ipaQuery || '%' AND r.ipaCode = :ipaCode AND r.text IN (:textList) LIMIT :limit")
    fun getListRoomBy(ipaQuery: String, ipaCode: String, textList: List<String>, limit: Int = Int.MAX_VALUE): List<Phonetic>


    @Query("SELECT r.* FROM $TABLE_NAME_FTS f JOIN $TABLE_NAME r ON f.rowid = r.rowid WHERE f.text MATCH :textQuery GROUP BY r.text LIMIT :limit")
    fun suggest(textQuery: String, limit: Int = 10): List<Phonetic>


    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()


    fun insertOrUpdate(entities: List<Phonetic>) {
        insertOrUpdateRoom(entities.map { it.toRoom() })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateRoom(room: RoomPhonetic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateRoom(rooms: List<RoomPhonetic>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateRoom(vararg rooms: RoomPhonetic)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["text", "ipaCode"],
    indices = [
        Index("text"),
        Index("ipaCode"),
        Index("ipaValue")
    ]
)
data class RoomPhonetic(
    val text: String,
    val ipaCode: String,
    val ipaValue: String,

    val extras: String
)

@Entity(tableName = TABLE_NAME_FTS)
@Fts4(contentEntity = RoomPhonetic::class)
data class RoomPhoneticFTS(
    val text: String,
    val ipaCode: String,
    val ipaValue: String,

    val extras: String
)

private fun Phonetic.toRoom() = RoomPhonetic(
    text = text.lowercase(),
    ipaCode = ipaCode.lowercase(),
    ipaValue = ipaValue.lowercase(),

    extras = this.toJson().lowercase(),
)

@Database(entities = [RoomPhonetic::class, RoomPhoneticFTS::class], version = 1, exportSchema = false)
abstract class PhoneticRoomDatabase : RoomDatabase() {

    abstract fun phoneticDao(): PhoneticDaoV2
}

object PhoneticProviderV2 {

    val phoneticDao by lazy {
        Room.databaseBuilder(PhoneticInitializer.application, PhoneticRoomDatabase::class.java, "phonetics_database_v2")
            .build().phoneticDao()
    }
}