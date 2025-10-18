package com.simple.feature.thanks.data.dao

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObject
import com.simple.feature.thanks.data.dao.RoomPendingThank.Companion.toEntity
import com.simple.feature.thanks.data.dao.RoomPendingThank.Companion.toRoom
import com.simple.feature.thanks.entities.Thank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val TABLE_NAME = "pending_thank_dao"

@Dao
interface PendingThankDao {

    fun getListAsync(): Flow<List<Thank>> = getRoomListAsync().map { list ->

        list.map {

            it.toEntity()
        }
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE 1=1")
    fun getRoomListAsync(): Flow<List<RoomPendingThank>>


    fun insertOrUpdate(thank: List<Thank>) {

        insertOrUpdateRoom(thank.map { it.toRoom() })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateRoom(rooms: List<RoomPendingThank>)
}

@Keep
@Entity(
    tableName = TABLE_NAME,
    primaryKeys = ["id"]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class RoomPendingThank(
    val id: String,
    val extra: String = "",
) {

    companion object {

        fun Thank.toRoom() = RoomPendingThank(
            id = id.orEmpty(),
            extra = this.toJson(),
        )

        fun RoomPendingThank.toEntity() = extra.toObject<Thank>()
    }
}