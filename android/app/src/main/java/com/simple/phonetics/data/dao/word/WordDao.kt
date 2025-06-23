package com.simple.phonetics.data.dao.word

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.simple.phonetics.data.dao.word.RoomWord.Companion.toEntity
import com.simple.phonetics.data.dao.word.RoomWord.Companion.toRoom
import com.simple.phonetics.entities.Word
import com.simple.phonetics.entities.Word.Resource.Companion.toResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "words"

@Dao
@Deprecated("")
interface WordDao {

    fun getListAsync(resource: String, languageCode: String): Flow<List<String>> = getRoomListAsync(resource = resource, languageCode = languageCode).map { list ->

        list.map {

            it.text
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE resource = :resource AND languageCode COLLATE NOCASE IN (:languageCode)")
    fun getRoomListAsync(resource: String, languageCode: String): Flow<List<RoomWord>>


    fun getRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<String> = getRoomRandom(resource = resource, languageCode = languageCode, textMin = textMin, textLimit = textLimit, limit = limit).map {
        it.text
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE resource = :resource AND languageCode COLLATE NOCASE IN (:languageCode) AND LENGTH(text) >= :textMin AND LENGTH(text) < :textLimit ORDER BY RANDOM() LIMIT :limit")
    fun getRoomRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<RoomWord>


    fun getAll(): List<Word> = getRoomAll().mapNotNull {

        it.toEntity()
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE 1=1")
    fun getRoomAll(): List<RoomWord>


    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE 1=1")
    fun getCount(): Int

    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE resource = :resource AND languageCode = :languageCode")
    fun getCountAsync(resource: String, languageCode: String): Flow<Int>


    fun insertOrUpdate(resource: String, languageCode: String, list: List<String>) {

        val rooms = list.map {
            it.toRoom(resource = resource, languageCode = languageCode)
        }

        insertOrUpdate(rooms)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomWord>)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["text"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomWord(
    val text: String,
    val resource: String,
    val languageCode: String = "",
) {

    companion object {

        fun String.toRoom(resource: String, languageCode: String) = RoomWord(
            text = this,
            resource = resource,
            languageCode = languageCode,
        )

        fun RoomWord.toEntity() = resource.toResource()?.let {

            Word(
                text = text,
                resource = it,
                languageCode = languageCode
            )
        }
    }
}

@Deprecated("")
@Database(entities = [RoomWord::class], version = 1, exportSchema = false)
abstract class WordRoomDatabase : RoomDatabase() {

    abstract fun providerWordDao(): WordDao
}

@Deprecated("")
class WordOldProvider(context: Context) {

    val wordDao by lazy {
        Room.databaseBuilder(context, WordRoomDatabase::class.java, "word_database")
            .build().providerWordDao()
    }
}
