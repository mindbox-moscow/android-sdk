package cloud.mindbox.mobile_sdk.models

import androidx.room.*
import cloud.mindbox.mobile_sdk.repository.MapTypeConverter
import java.util.*

@Entity
data class RoomEvent(
    @ColumnInfo(name = "operation") var operation: String,
    @ColumnInfo(name = "endpoint") var endpoint: String,
    @ColumnInfo(name = "transactionId") var transactionId: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "enqueueTimestamp") var enqueueTimestamp: Long = Date().time, // date of event creating
    @TypeConverters(MapTypeConverter::class)
    @ColumnInfo(name = "additionalFields") var additionalFields: Map<String, String>? = null,
    @ColumnInfo(name = "body") var body: String? = null //json
) {

    @PrimaryKey var uid: String = "${enqueueTimestamp};${transactionId}"

}
