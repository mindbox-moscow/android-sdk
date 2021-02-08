package cloud.mindbox.mobile_sdk.managers

import cloud.mindbox.mobile_sdk.Configuration
import cloud.mindbox.mobile_sdk.Logger
import cloud.mindbox.mobile_sdk.models.Event
import io.paperdb.Paper
import io.paperdb.PaperDbException
import java.util.*

// todo add run catching

internal object DbManager {

    private const val CONFIGURATION_KEY = "configuration_key"
    private const val EVENTS_BOOK_NAME = "mindbox_events_book"
    private const val CONFIGURATION_BOOK_NAME = "mindbox_configuration_book"

    private const val MAX_EVENT_LIST_SIZE = 10000
    private const val HALF_YEAR_IN_MILLISECONDS: Long = 15552000000L

    private val eventsBook = Paper.book(EVENTS_BOOK_NAME)
    private val configurationBook = Paper.book(CONFIGURATION_BOOK_NAME)

    fun addEventToQueue(event: Event) {
        synchronized(this) {
            try {
                filterEventsBySize()
                filterOldEvents()

                eventsBook.write(event.transactionId, event)
                Logger.d(this, "Event ${event.eventType.type} was added to queue")
            } catch (exception: PaperDbException) {
                Logger.e(
                    this,
                    "Error writing object to the database: ${event.body}",
                    exception
                )
            }
        }
    }

    fun getEventsKeys(): List<String> = synchronized(this) {
        return eventsBook.allKeys
    }

    fun getEvent(key: String): Event? {
        synchronized(this) {
            return try {
                eventsBook.read(key) as Event?
            } catch (exception: PaperDbException) {

                // invalid data in case of exception
                removeEventFromQueue(key)
                Logger.e(this, "Error reading from database", exception)
                null
            }
        }
    }

    fun removeEventFromQueue(key: String) {
        synchronized(this) {
            try {
                eventsBook.delete(key)
            } catch (exception: PaperDbException) {
                Logger.e(this, "Error deleting item from database", exception)
            }
        }
    }

    private fun filterEventsBySize() {
        synchronized(this) {
            val allKeys = getEventsKeys()

            if (allKeys.size >= MAX_EVENT_LIST_SIZE) {
                removeEventFromQueue(allKeys.first())
            }
        }
    }

    private fun filterOldEvents() {
        synchronized(this) {
            val keys = getEventsKeys()
            keys.forEach { key ->
                val event = getEvent(key)
                if (event?.isTooOld() == true) {
                    removeEventFromQueue(key)
                } else {
                    return@forEach
                }
            }
        }
    }

    private fun Event.isTooOld(): Boolean =
        this.enqueueTimestamp - Date().time >= HALF_YEAR_IN_MILLISECONDS

    fun saveConfigurations(configuration: Configuration) {
        synchronized(this) {
            try {
                configurationBook.write(CONFIGURATION_KEY, configuration)
            } catch (exception: PaperDbException) {
                Logger.e(this, "Error writing object configuration to the database", exception)
            }
        }
    }

    fun getConfigurations(): Configuration? {
        synchronized(this) {
            return try {
                configurationBook.read(CONFIGURATION_KEY) as Configuration?
            } catch (exception: PaperDbException) {

                // invalid data in case of exception
                Logger.e(this, "Error reading from database", exception)
                null
            }
        }
    }
}