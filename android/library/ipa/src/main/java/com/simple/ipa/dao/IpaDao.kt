package com.simple.ipa.dao

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
import com.simple.ipa.entities.Ipa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "ipa_dao"

@Dao
interface IpaDaoV2 {

    fun getListAsync(languageCode: String): Flow<List<Ipa>> = getRoomListAsync(languageCode = languageCode).map { list ->

        list.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE languageCode COLLATE NOCASE IN (:languageCode)")
    fun getRoomListAsync(languageCode: String): Flow<List<RoomIpaNew>>


    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE languageCode = :languageCode")
    fun getCount(languageCode: String): Int

    @Query("SELECT COUNT(*) FROM $TABLE_NAME WHERE languageCode = :languageCode")
    fun getCountAsync(languageCode: String): Flow<Int>


    fun insertOrUpdate(languageCode: String, list: List<Ipa>) {

        val rooms = list.map {
            it.toRoomNew(languageCode = languageCode)
        }

        insertOrUpdate(rooms)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(rooms: List<RoomIpaNew>)


    @Query("DELETE FROM $TABLE_NAME WHERE ipa = :ipa AND languageCode = :languageCode")
    fun deleteByKey(ipa: String, languageCode: String)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["ipa", "languageCode"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomIpaNew(
    val ipa: String,
    val languageCode: String = "",

    val extra: String = ""
)


private fun Ipa.toRoomNew(languageCode: String) = RoomIpaNew(
    ipa = ipa,
    languageCode = languageCode,

    extra = this.toJson()
)

private fun RoomIpaNew.toEntity() = extra.toObject<Ipa>()


@Database(entities = [RoomIpaNew::class], version = 1, exportSchema = false)
abstract class IpaRoomDatabaseNew : RoomDatabase() {

    abstract fun providerIpaDao(): IpaDaoV2
}


class IpaProviderV2(context: Context) {

    val ipaDao by lazy {
        Room.databaseBuilder(context, IpaRoomDatabaseNew::class.java, "ipa_database_new")
            .build().providerIpaDao()
    }
}