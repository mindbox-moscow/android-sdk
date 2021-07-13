package cloud.mindbox.mobile_sdk

import android.content.Context
import android.os.Build
import cloud.mindbox.mobile_sdk.logger.MindboxLogger
import cloud.mindbox.mobile_sdk.managers.SharedPreferencesManager
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences

/**
 * The configuration object used to initialize Mindbox SDK
 * The parameters are taken into account only during first initialization
 */
class MindboxConfiguration(builder: Builder) {
    internal val previousInstallationId: String = builder.previousInstallationId
    internal var previousDeviceUUID: String = builder.previousDeviceUUID
    internal val endpointId: String = builder.endpointId
    internal val domain: String = builder.domain
    internal val packageName: String = builder.packageName
    internal val versionName: String = builder.versionName
    internal val versionCode: String = builder.versionCode
    internal val subscribeCustomerIfCreated: Boolean = builder.subscribeCustomerIfCreated
    internal val shouldCreateCustomer: Boolean = builder.shouldCreateCustomer

    /**
     * A Builder for MindboxConfiguration
     */
    class Builder(private val context: Context, val domain: String, val endpointId: String) {
        internal var previousInstallationId: String = ""
        internal var previousDeviceUUID: String = ""
        internal var subscribeCustomerIfCreated: Boolean = false
        internal var packageName: String = PLACEHOLDER_APP_PACKAGE_NAME
        internal var versionName: String = PLACEHOLDER_APP_VERSION_NAME
        internal var versionCode: String = PLACEHOLDER_APP_VERSION_CODE
        internal var shouldCreateCustomer: Boolean = true

        companion object {
            private const val PLACEHOLDER_APP_PACKAGE_NAME = "Unknown package name"
            private const val PLACEHOLDER_APP_VERSION_NAME = "Unknown version"
            private const val PLACEHOLDER_APP_VERSION_CODE = "?"
        }

        /**
         * Specifies deviceUUID for Mindbox
         *
         * @param previousDeviceUUID - deprecate - old device id which was used to find a customer by the device in our DB
         */
        fun setPreviousDeviceUuid(previousDeviceUUID: String): Builder {
            this.previousDeviceUUID = previousDeviceUUID
            return this
        }

        /**
         * Specifies installationId for Mindbox
         *
         * @param previousInstallationId - deprecate - old id which was used to send mobile push
         */
        fun setPreviousInstallationId(previousInstallationId: String): Builder {
            this.previousInstallationId = previousInstallationId
            return this
        }

        /**
         * Specifies subscribeCustomerIfCreated for Mindbox
         *
         * @param subscribe - flag which determines subscription status of the user
         */
        fun subscribeCustomerIfCreated(subscribe: Boolean): Builder {
            this.subscribeCustomerIfCreated = subscribe
            return this
        }

        /**
         * Specifies shouldCreateCustomer for Mindbox. Usable only during first initialisation
         *
         * @param shouldCreateCustomer - flag which determines create or not anonymous users.
         * Default value is true.
         */
        fun shouldCreateCustomer(shouldCreateCustomer: Boolean): Builder {
            this.shouldCreateCustomer = shouldCreateCustomer
            return this
        }

        /**
         * Creates a new MindboxConfiguration.Builder.
         */
        fun build(): MindboxConfiguration {
            generateAppInfo(context)
            return MindboxConfiguration(this)
        }

        private fun generateAppInfo(context: Context) {
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
                packageName = packageInfo.packageName.trim()
                this.versionName = packageInfo.versionName?.trim() ?: PLACEHOLDER_APP_PACKAGE_NAME
                this.versionCode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString().trim()
                    } else {
                        packageInfo.versionCode.toString().trim()
                    }

                //need for scheduling and stopping one-time background service
                SharedPreferencesManager.with(context)
                MindboxPreferences.hostAppName = packageName

            } catch (e: Exception) {
                MindboxLogger.e(
                    this,
                    "Getting app info failed. Identified as an unknown application"
                )
            }
        }
    }
}