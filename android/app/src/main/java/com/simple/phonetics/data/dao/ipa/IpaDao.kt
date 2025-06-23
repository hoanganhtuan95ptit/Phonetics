package com.simple.phonetics.data.dao.ipa

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
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObject
import com.simple.dao.entities.Ipa
import com.simple.phonetics.data.dao.ipa.RoomIpa.Companion.toEntity
import com.simple.phonetics.data.dao.ipa.RoomIpa.Companion.toRoom
import com.simple.phonetics.data.dao.word.WordRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "ipas"

@Dao
@Deprecated("")
interface IpaDao {

    fun getListAsync(languageCode: String): Flow<List<Ipa>> = getRoomListAsync(languageCode = languageCode).map { list ->

        list.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE languageCode COLLATE NOCASE IN (:languageCode)")
    fun getRoomListAsync(languageCode: String): Flow<List<RoomIpa>>


    fun insertOrUpdate(languageCode: String, list: List<Ipa>) {

        val rooms = list.map {
            it.toRoom(languageCode = languageCode)
        }

        insertOrUpdate(rooms)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomIpa>)

    
    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE languageCode = :languageCode")
    fun getCount( languageCode: String): Int

    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE languageCode = :languageCode")
    fun getCountAsync(languageCode: String): Flow<Int>
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["ipa"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomIpa(
    val ipa: String,

    val examples: String = "",
    val languageCode: String = "",

    val type: String = "",
    val voice: String = "",
) {

    companion object {

        fun Ipa.toRoom(languageCode: String) = RoomIpa(
            ipa = ipa,
            examples = examples.toJson(),
            languageCode = languageCode,

            type = type,
            voice = voice
        )

        fun RoomIpa.toEntity() = Ipa(
            ipa = ipa,
            examples = examples.toObject<List<String>>(),

            type = type,
            voice = voice
        )
    }
}

@Deprecated("")
@Database(entities = [RoomIpa::class], version = 1, exportSchema = false)
abstract class IpaRoomDatabase : RoomDatabase() {

    abstract fun providerIpaDao(): IpaDao
}


@Deprecated("")
class IpaOldProvider(context: Context) {

    val ipaDao by lazy {
        Room.databaseBuilder(context, IpaRoomDatabase::class.java, "ipa_database")
            .build().providerIpaDao()
    }
}
