package com.simple.okhttp.cache.data.dao

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
import com.simple.dao.ipa.RoomIpaNew
import com.simple.okhttp.cache.entities.NetworkEntity

private const val TABLE_NAME = "network_dao"

@Dao
interface NetworkDao {

    fun getOrNullEntity(id: String): NetworkEntity? = getOrNull(id)?.toEntity()

    @Query("SELECT * FROM $TABLE_NAME WHERE id COLLATE NOCASE IN (:id)")
    fun getOrNull(id: String): RoomNetwork?


    fun insertEntity(entity: NetworkEntity) = insert(entity.toRoom())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: RoomNetwork)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["id"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomNetwork(
    val id: String,

    val extra: String = ""
)

private fun NetworkEntity.toRoom() = RoomNetwork(
    id = id,

    extra = this.toJson()
)

private fun RoomNetwork.toEntity() = extra.toObject<NetworkEntity>()


@Database(entities = [RoomNetwork::class], version = 1, exportSchema = false)
abstract class NetworkRoomDatabaseNew : RoomDatabase() {

    abstract fun networkDao(): NetworkDao
}


class NetworkProvider(context: Context) {

    val networkDao by lazy {
        Room.databaseBuilder(context, NetworkRoomDatabaseNew::class.java, "network_database")
            .build().networkDao()
    }
}