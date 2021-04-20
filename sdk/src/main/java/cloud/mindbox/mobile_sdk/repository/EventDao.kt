package cloud.mindbox.mobile_sdk.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import cloud.mindbox.mobile_sdk.models.RoomEvent

@Dao
interface EventDao {
    @Query("SELECT uid FROM roomevent")
    fun getAllKeys(): List<String>

    @Query("SELECT * FROM roomevent")
    fun getAll(): List<RoomEvent>

    @Query("SELECT * FROM roomevent WHERE uid = :eventId")
    fun loadById(eventId: String): RoomEvent

    @Insert
    fun insertAll(vararg events: RoomEvent)

    @Delete
    fun delete(events: RoomEvent)

}
