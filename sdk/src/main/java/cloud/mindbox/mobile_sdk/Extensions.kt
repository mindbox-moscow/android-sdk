package cloud.mindbox.mobile_sdk

import cloud.mindbox.mobile_sdk.logger.MindboxLogger
import java.util.*

internal fun <T> Result<T>.returnOnException(block: (exception: Throwable) -> T): T {
    return this.getOrElse { exception ->
        exception.handle()
        return block.invoke(exception)
    }
}

internal fun Result<Unit>.logOnException() {
    this.exceptionOrNull()?.handle()
}

private fun Throwable.handle() {
    try {
        MindboxLogger.e(Mindbox, "Mindbox caught unhandled error", this)
        // todo log crash
    } catch (e: Throwable) {
    }
}

internal fun String.isUuid(): Boolean {
    return if (this.trim().isNotEmpty()) {
        try {
            UUID.fromString(this)
            true
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}

internal fun Map<String, String>.toUrlQueryString() = runCatching {
    return this.map { (k, v) -> "$k=$v" }
        .joinToString(prefix = "?", separator = "&")
}.returnOnException { "" }
