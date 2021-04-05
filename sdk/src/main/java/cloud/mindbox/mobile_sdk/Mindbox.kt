package cloud.mindbox.mobile_sdk

import android.content.Context
import cloud.mindbox.mobile_sdk.managers.DbManager
import cloud.mindbox.mobile_sdk.managers.IdentifierManager
import cloud.mindbox.mobile_sdk.managers.MindboxEventManager
import cloud.mindbox.mobile_sdk.models.InitData
import cloud.mindbox.mobile_sdk.models.TrackClickData
import cloud.mindbox.mobile_sdk.models.UpdateData
import cloud.mindbox.mobile_sdk.models.ValidationError
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences
import com.google.firebase.FirebaseApp
import com.orhanobut.hawk.Hawk
import io.paperdb.Paper
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Mindbox {

    private val mindboxJob = Job()
    private val mindboxScope = CoroutineScope(Default + mindboxJob)
    private val deviceUuidCallbacks = ConcurrentHashMap<String, (String) -> Unit>()
    private val fmsTokenCallbacks = ConcurrentHashMap<String, (String?) -> Unit>()

    /**
     * Subscribe to gets token of Firebase Messaging Service used by SDK
     *
     * @param subscription - invocation function with FMS token
     * @return String identifier of subscription
     * @see disposeFmsTokenSubscription
     */
    fun subscribeFmsToken(subscription: (String?) -> Unit): String {
        val subscriptionId = UUID.randomUUID().toString()

        if (Hawk.isBuilt() && !MindboxPreferences.isFirstInitialize) {
            subscription.invoke(MindboxPreferences.firebaseToken)
        } else {
            fmsTokenCallbacks[subscriptionId] = subscription
        }

        return subscriptionId
    }

    /**
     * Removes FMS token subscription if it is no longer necessary
     *
     * @param subscriptionId - identifier of the subscription to remove
     */
    fun disposeFmsTokenSubscription(subscriptionId: String) {
        fmsTokenCallbacks.remove(subscriptionId)
    }

    /**
     * Returns date of FMS token saving
     */
    fun getFmsTokenSaveDate(): String =
        runCatching { return MindboxPreferences.firebaseTokenSaveDate }
            .returnOnException { "" }

    /**
     * Returns SDK version
     */
    fun getSdkVersion(): String = runCatching { return BuildConfig.VERSION_NAME }
        .returnOnException { "" }

    /**
     * Subscribe to gets deviceUUID used by SDK
     *
     * @param subscription - invocation function with deviceUUID
     * @return String identifier of subscription
     * @see disposeDeviceUuidSubscription
     */
    fun subscribeDeviceUuid(context: Context, subscription: (String) -> Unit): String {
        initComponents(context)

        val subscriptionId = UUID.randomUUID().toString()
        val configuration = DbManager.getConfigurations()

        if (configuration != null && !MindboxPreferences.isFirstInitialize) {
            subscription.invoke(configuration.deviceUuid)
        } else {
            deviceUuidCallbacks[subscriptionId] = subscription
        }

        return subscriptionId
    }

    /**
     * Removes deviceUuid subscription if it is no longer necessary
     *
     * @param subscriptionId - identifier of the subscription to remove
     */
    fun disposeDeviceUuidSubscription(subscriptionId: String) {
        deviceUuidCallbacks.remove(subscriptionId)
    }

    /**
     * Updates FMS token for SDK
     * Call it from onNewToken on messaging service
     *
     * @param context used to initialize the main tools
     * @param token - token of FMS
     */
    fun updateFmsToken(context: Context, token: String) {
        runCatching {
            if (token.trim().isNotEmpty()) {
                initComponents(context)

                if (!MindboxPreferences.isFirstInitialize) {
                    mindboxScope.launch {
                        updateAppInfo(context, token)
                    }
                }
            }
        }.logOnException()
    }

    /**
     * Creates and deliveries event of "Push delivered"
     *
     * @param context used to initialize the main tools
     * @param uniqKey - unique identifier of push notification
     */
    fun onPushReceived(context: Context, uniqKey: String) {
        runCatching {
            initComponents(context)
            MindboxEventManager.pushDelivered(context, uniqKey)

            if (!MindboxPreferences.isFirstInitialize) {
                mindboxScope.launch {
                    updateAppInfo(context)
                }
            }
        }.logOnException()
    }

    /**
     * Creates and deliveries event of "Push clicked"
     *
     * @param context used to initialize the main tools
     * @param uniqKey - unique identifier of push notification
     * @param buttonUniqKey - unique identifier of push notification button
     */
    fun onPushClicked(context: Context, uniqKey: String, buttonUniqKey: String) {
        runCatching {
            initComponents(context)
            MindboxEventManager.pushClicked(context, TrackClickData(uniqKey, buttonUniqKey))

            if (!MindboxPreferences.isFirstInitialize) {
                mindboxScope.launch {
                    updateAppInfo(context)
                }
            }
        }.logOnException()
    }

    /**
     * Initializes the SDK for further work.
     * We recommend calling it in onCreate on an application class
     *
     * @param context used to initialize the main tools
     * @param configuration contains the data that is needed to connect to the Mindbox
     */
    fun init(
        context: Context,
        configuration: MindboxConfiguration
    ) {
        runCatching {
            initComponents(context)

            val validationErrors =
                ValidationError()
                    .apply {
                        validateFields(
                            configuration.domain,
                            configuration.endpointId,
                            configuration.deviceUuid,
                            configuration.installationId
                        )
                    }

            validationErrors.messages
                ?: throw InitializeMindboxException(validationErrors.messages.toString())

            mindboxScope.launch {

                if (MindboxPreferences.isFirstInitialize) {

                    if (configuration.deviceUuid.trim().isEmpty()) {
                        configuration.deviceUuid = initDeviceId(context)
                    } else {
                        configuration.deviceUuid.trim()
                    }

                    firstInitialization(context, configuration)
                } else {
                    updateAppInfo(context)
                    MindboxEventManager.sendEventsIfExist(context)
                }
            }
        }.returnOnException { }
    }

    internal fun initComponents(context: Context) {
        if (!Hawk.isBuilt()) Hawk.init(context).build()
        Paper.init(context)
        FirebaseApp.initializeApp(context)
    }

    private suspend fun initDeviceId(context: Context): String {
        val adid = mindboxScope.async { IdentifierManager.getAdsIdentification(context) }
        return adid.await()
    }

    private suspend fun firstInitialization(context: Context, configuration: MindboxConfiguration) {
        runCatching {
            val firebaseToken = withContext(mindboxScope.coroutineContext) {
                IdentifierManager.registerFirebaseToken()
            }

            val isNotificationEnabled = IdentifierManager.isNotificationsEnabled(context)

            DbManager.saveConfigurations(configuration)

            val isTokenAvailable = !firebaseToken.isNullOrEmpty()
            val initData = InitData(
                token = firebaseToken ?: "",
                isTokenAvailable = isTokenAvailable,
                installationId = configuration.installationId,
                isNotificationsEnabled = isNotificationEnabled,
                subscribe = configuration.subscribeCustomerIfCreated
            )

            MindboxEventManager.appInstalled(context, initData)

            MindboxPreferences.isFirstInitialize = false
            MindboxPreferences.firebaseToken = firebaseToken
            MindboxPreferences.isNotificationEnabled = isNotificationEnabled

            deliverDeviceUuid(configuration.deviceUuid)
            deliverFmsToken(firebaseToken)
        }.logOnException()
    }

    private suspend fun updateAppInfo(context: Context, token: String? = null) {
        runCatching {
            val firebaseToken = token
                ?: withContext(mindboxScope.coroutineContext) { IdentifierManager.registerFirebaseToken() }

            val isTokenAvailable = !firebaseToken.isNullOrEmpty()

            val isNotificationEnabled = IdentifierManager.isNotificationsEnabled(context)

            if ((isTokenAvailable && firebaseToken != MindboxPreferences.firebaseToken) || isNotificationEnabled != MindboxPreferences.isNotificationEnabled) {

                val initData = UpdateData(
                    token = firebaseToken ?: "",
                    isTokenAvailable = isTokenAvailable,
                    isNotificationsEnabled = isNotificationEnabled
                )

                MindboxEventManager.appInfoUpdate(context, initData)

                MindboxPreferences.isNotificationEnabled = isNotificationEnabled
                MindboxPreferences.firebaseToken = firebaseToken
            }
        }.logOnException()
    }

    private fun deliverDeviceUuid(deviceUuid: String) {
        Executors.newSingleThreadScheduledExecutor().schedule({
            deviceUuidCallbacks.keys.forEach { key ->
                deviceUuidCallbacks[key]?.invoke(deviceUuid)
                deviceUuidCallbacks.remove(key)
            }
        }, 1, TimeUnit.SECONDS)
    }

    private fun deliverFmsToken(token: String?) {
        Executors.newSingleThreadScheduledExecutor().schedule({
            fmsTokenCallbacks.keys.forEach { key ->
                fmsTokenCallbacks[key]?.invoke(token)
                fmsTokenCallbacks.remove(key)
            }
        }, 1, TimeUnit.SECONDS)
    }
}