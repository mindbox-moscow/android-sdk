package cloud.mindbox.mobile_sdk.repository

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MapTypeConverter {

    @TypeConverter
    @JvmStatic
    fun stringToMap(value: String): Map<String, String> {
        return Gson().fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
    }

    @TypeConverter
    @JvmStatic
    fun mapToString(value: Map<String, String>?): String {
        return if (value == null) "" else Gson().toJson(value)
    }
}
