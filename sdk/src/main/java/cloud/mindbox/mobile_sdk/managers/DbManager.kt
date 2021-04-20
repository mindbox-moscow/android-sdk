package cloud.mindbox.mobile_sdk.managers

import android.content.Context
import androidx.room.Room
import cloud.mindbox.mobile_sdk.MindboxConfiguration
import cloud.mindbox.mobile_sdk.logger.MindboxLogger
import cloud.mindbox.mobile_sdk.logOnException
import cloud.mindbox.mobile_sdk.models.Event
import cloud.mindbox.mobile_sdk.models.RoomEvent
import cloud.mindbox.mobile_sdk.repository.EventsDatabase
import cloud.mindbox.mobile_sdk.returnOnException
import cloud.mindbox.mobile_sdk.services.BackgroundWorkManager
import io.paperdb.Paper
import io.paperdb.PaperDbException
import java.util.*
import kotlin.collections.ArrayList

internal object DbManager {

    private const val CONFIGURATION_KEY = "configuration_key"
    private const val EVENTS_BOOK_NAME = "mindbox_events_book"
    private const val CONFIGURATION_BOOK_NAME = "mindbox_configuration_book"

    private const val MAX_EVENT_LIST_SIZE = 10000
    private const val HALF_YEAR_IN_MILLISECONDS: Long = 15552000000L

    private val eventsBook = Paper.book(EVENTS_BOOK_NAME)
    private val configurationBook = Paper.book(CONFIGURATION_BOOK_NAME)

    private var writePaperTime = 0L
    private var writeRoomTime = 0L

    private lateinit var roomDb: EventsDatabase

    fun addEventToQueue(context: Context, event: Event) {
        runCatching {
            try {
                eventsBook.write("${event.enqueueTimestamp};${event.transactionId}", event)
                MindboxLogger.d(this, "Event ${event.eventType.operation} was added to queue")
            } catch (exception: PaperDbException) {
                MindboxLogger.e(
                    this,
                    "Error writing object to the database: ${event.body}",
                    exception
                )
            }

            //BackgroundWorkManager.startOneTimeService(context)
        }.logOnException()
    }

    fun getFilteredEventsKeys(): List<String> = runCatching {
        return sortKeys(getEventsKeys())
            .filterOldEvents()
            .filterEventsBySize()
            .toList()
    }.returnOnException { emptyList() }

    fun addEventToPaperQueue(context: Context, event: RoomEvent) {
        runCatching {
            try {
                val start = System.currentTimeMillis()
                eventsBook.write(event.uid, event)
                writePaperTime += System.currentTimeMillis() - start
                MindboxLogger.d(
                    this,
                    "Event ${event.operation} was added to queue time $writePaperTime"
                )
            } catch (exception: PaperDbException) {
                MindboxLogger.e(
                    this,
                    "Error writing object to the database: ${event.body}",
                    exception
                )
            }
        }.logOnException()
    }

    fun testPaperRead() {
        val keys = getFilteredEventsKeys()
        val start = System.currentTimeMillis()
        keys.forEach {
            try {
                eventsBook.read(it) as? RoomEvent
            } catch (exception: PaperDbException) {
                MindboxLogger.e(this, "Error reading from database", exception)
            }
        }
        MindboxLogger.w(
            this,
            "Events were write $writePaperTime and were read ${System.currentTimeMillis() - start}"
        )
        writePaperTime = 0L
    }

    fun addEventToRoomQueue(context: Context, event: RoomEvent) {
        runCatching {
            try {
                if (!this::roomDb.isInitialized) {
                    roomDb = Room.databaseBuilder(
                        context.applicationContext,
                        EventsDatabase::class.java, "events_db"
                    )
                        .build()
                }

                val start = System.currentTimeMillis()
                roomDb.eventsDao().insertAll(event)
                writeRoomTime += System.currentTimeMillis() - start
                MindboxLogger.d(
                    this,
                    "Room event ${event.operation} was added to queue time $writeRoomTime"
                )
            } catch (exception: PaperDbException) {
                MindboxLogger.e(
                    this,
                    "Error writing object to the Room database: ${event.body}",
                    exception
                )
            }
        }.logOnException()
    }

    fun testRoomRead() {
        val keys = sortKeys(getRoomEventsKeys())
            .filterOldEvents()
            .filterEventsBySize()
            .toList()
        val start = System.currentTimeMillis()
        keys.forEach { roomDb.eventsDao().loadById(it) }
        val timeAsPaper = System.currentTimeMillis() - start
        val startAll = System.currentTimeMillis()
        roomDb.eventsDao().getAll()
        val timeAll = System.currentTimeMillis() - startAll
        MindboxLogger.w(
            this,
            "Room events were write $writeRoomTime and were read as paper $timeAsPaper and read all $timeAll"
        )
        writeRoomTime = 0L
    }

    private fun getRoomEventsKeys(): List<String> {
        return runCatching {
            val start = System.currentTimeMillis()
            val keys = roomDb.eventsDao().getAllKeys()
            MindboxLogger.w(this, "Room keys were read ${System.currentTimeMillis() - start}")
            return keys
        }.returnOnException { emptyList() }
    }

    private fun getEventsKeys(): List<String> {
        return runCatching {
            val start = System.currentTimeMillis()
            val keys = eventsBook.allKeys
            MindboxLogger.w(this, "Keys were read ${System.currentTimeMillis() - start}")
            return keys
        }.returnOnException { emptyList() }
    }

    fun getEvent(key: String): Event? {
        return runCatching {
            return try {
                eventsBook.read(key) as? Event
            } catch (exception: PaperDbException) {

                // invalid data in case of exception
                removeEventFromQueue(key)
                MindboxLogger.e(this, "Error reading from database", exception)
                null
            }
        }.returnOnException { null }
    }

    fun removeEventFromQueue(key: String) {
        runCatching {
            try {
                eventsBook.delete(key)
                MindboxLogger.d(this, "Event $key was deleted from queue")
            } catch (exception: PaperDbException) {
                MindboxLogger.e(this, "Error deleting item from database", exception)
            }
        }.logOnException()
    }

    private fun sortKeys(list: List<String>): ArrayList<String> {
        val arrayList = ArrayList<String>()
        runCatching {
            arrayList.addAll(list)

            arrayList.sortBy { key ->
                key.getTimeFromKey()
            }
        }.logOnException()
        return arrayList
    }

    private fun ArrayList<String>.filterEventsBySize(): ArrayList<String> {
        return runCatching {

            val diff = this.size - MAX_EVENT_LIST_SIZE

            return if (diff > 0) { // allKeys.size >= MAX_EVENT_LIST_SIZE
                val filteredList = ArrayList(this) //coping of list
                for (i in 1..diff) {
                    removeEventFromQueue(this[i])
                    filteredList.remove(this[i])
                }
                filteredList
            } else {
                this
            }
        }.returnOnException { arrayListOf() }
    }

    private fun ArrayList<String>.filterOldEvents(): ArrayList<String> {
        return runCatching {
            val filteredList = ArrayList(this) //coping of list
            val timeNow = Date().time
            this.forEach { key ->
                if (key.isTooOldKey(timeNow)) {
                    removeEventFromQueue(key)
                    filteredList.remove(key)
                }
            }
            return filteredList
        }.returnOnException { arrayListOf() }
    }

    private fun String.isTooOldKey(timeNow: Long): Boolean {
        return runCatching {
            return this.getTimeFromKey() - timeNow >= HALF_YEAR_IN_MILLISECONDS
        }.returnOnException { false }
    }

    fun saveConfigurations(configuration: MindboxConfiguration) {
        runCatching {
            try {
                configurationBook.write(CONFIGURATION_KEY, configuration)
            } catch (exception: PaperDbException) {
                MindboxLogger.e(
                    this,
                    "Error writing object configuration to the database",
                    exception
                )
            }
        }.returnOnException { }
    }

    fun getConfigurations(): MindboxConfiguration? {
        return runCatching {
            try {
                return configurationBook.read(CONFIGURATION_KEY) as MindboxConfiguration?
            } catch (exception: PaperDbException) {

                // invalid data in case of exception
                MindboxLogger.e(this, "Error reading from database", exception)
                return null
            }
        }.returnOnException { null }
    }

    private fun String.getTimeFromKey(): Long {
        val keyTimeStamp = this.substringBefore(";", "0")

        return try {
            keyTimeStamp.toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }
}