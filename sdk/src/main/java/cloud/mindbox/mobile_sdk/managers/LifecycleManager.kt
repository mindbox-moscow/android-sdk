package cloud.mindbox.mobile_sdk.managers

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.logOnException
import java.util.*
import kotlin.concurrent.timer

internal class LifecycleManager(
    private val onAppStarted: () -> Unit
) : Application.ActivityLifecycleCallbacks, ComponentCallbacks2, LifecycleObserver {

    companion object {

        private const val TIMER_PERIOD = 1200000L

    }

    private var currentActivity: Activity? = null
    private var isConfigurationChanged = false
    private var timer: Timer? = null

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        when {
            isConfigurationChanged -> isConfigurationChanged = false
            currentActivity?.javaClass?.name == activity.javaClass.name -> {
                onAppStarted()
                startKeepAliveTimer(activity.applicationContext)
            }
            else -> currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == null) {
            currentActivity = activity
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        isConfigurationChanged = true
    }

    override fun onLowMemory() {

    }

    override fun onTrimMemory(level: Int) {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppMovedToBackground() {
        cancelKeepAliveTimer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppMovedToForeground() {

    }

    private fun startKeepAliveTimer(context: Context) = runCatching {
        cancelKeepAliveTimer()
        val endpointId = DbManager.getConfigurations()?.endpointId
        if (endpointId != null) {
            timer = timer(
                initialDelay = TIMER_PERIOD,
                period = TIMER_PERIOD,
                action = { Mindbox.sendTrackVisitEvent(context.applicationContext, endpointId) }
            )
        }
    }.logOnException()

    private fun cancelKeepAliveTimer() = runCatching {
        timer?.cancel()
        timer = null
    }.logOnException()

}
