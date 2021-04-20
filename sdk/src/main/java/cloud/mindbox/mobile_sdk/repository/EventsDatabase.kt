package cloud.mindbox.mobile_sdk.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cloud.mindbox.mobile_sdk.models.RoomEvent

@Database(entities = [RoomEvent::class], version = 1)
@TypeConverters(MapTypeConverter::class)
abstract class EventsDatabase : RoomDatabase() {

    abstract fun eventsDao(): EventDao

}
