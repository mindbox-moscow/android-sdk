package cloud.mindbox.mobile_sdk.managers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

internal object SharedPreferencesManager {

    private const val FILE_NAME = "preferences"
    private const val DEFAULT_INT_VALUE = -1

    private lateinit var preferences: SharedPreferences

    /**
     * Call this first before retrieving or saving object.
     *
     * @param context The context is used.
     */
    fun with(context: Context) {
        if (!isInitialized()) {
            val application = context.applicationContext as? Application
            application?.let {
                preferences = it.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Check were shared preferences initialized.
     */
    fun isInitialized() = this::preferences.isInitialized

    /**
     * Saves [String] into the Preferences.
     *
     * @param key Key with which Shared preferences to
     * @param value Object of [String] class to save
     **/
    fun put(
        key: String,
        value: String?
    ) = preferences.edit().putString(key, value).apply()

    /**
     * Saves [Boolean] into the Preferences.
     *
     * @param key Key with which Shared preferences to
     * @param value Object of [Boolean] class to save
     **/
    fun put(
        key: String,
        value: Boolean
    ) = preferences.edit().putBoolean(key, value).apply()

    /**
     * Saves [Int] into the Preferences.
     *
     * @param key Key with which Shared preferences to
     * @param value Object of [Int] class to save
     **/
    fun put(
        key: String,
        value: Int
    ) = preferences.edit().putInt(key, value).apply()

    /**
     * Used to retrieve [String] object from the Preferences.
     *
     * @param key Shared Preference key with which object was saved.
     * @param defaultValue Object of [String] class to return if there is no value. Default value
     * is null.
     **/
    fun getString(
        key: String,
        defaultValue: String? = null
    ): String? = preferences.getString(key, defaultValue)

    /**
     * Used to retrieve [Boolean] object from the Preferences.
     *
     * @param key Shared Preference key with which object was saved.
     * @param defaultValue Object of [Boolean] class to return if there is no value. Default value
     * is false.
     **/
    fun getBoolean(
        key: String,
        defaultValue: Boolean = false
    ): Boolean = preferences.getBoolean(key, defaultValue)

    /**
     * Used to retrieve [Int] object from the Preferences.
     *
     * @param key Shared Preference key with which object was saved.
     * @param defaultValue Object of [Int] class to return if there is no value. Default value
     * is [DEFAULT_INT_VALUE].
     **/
    fun getInt(
        key: String,
        defaultValue: Int = DEFAULT_INT_VALUE
    ): Int = preferences.getInt(key, defaultValue)

    internal fun deleteAll() = preferences.edit().clear().apply()

}
