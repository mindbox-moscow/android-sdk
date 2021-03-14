package cloud.mindbox.mobile_sdk.services

import android.content.Context
import androidx.work.*
import cloud.mindbox.mobile_sdk.logOnException
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences
import cloud.mindbox.mobile_sdk.returnOnException
import java.util.concurrent.TimeUnit

internal object BackgroundWorkManager {

    private val ONE_TIME_WORKER_TAG =
        MindboxOneTimeEventWorker::class.java.simpleName + MindboxPreferences.hostAppName
    private val PERIODIC_WORKER_TAG =
        PeriodicWorkRequest::class.java.simpleName + MindboxPreferences.hostAppName

    fun startPeriodicService(context: Context) {
        runCatching {
            val request = PeriodicWorkRequest.Builder(
                MindboxPeriodicEventWorker::class.java,
                1, TimeUnit.HOURS
            )
                .setInitialDelay(1, TimeUnit.HOURS)
                .addTag(PERIODIC_WORKER_TAG)
//                .setBackoffCriteria(
//                    BackoffPolicy.LINEAR,
//                    1,
//                    TimeUnit.HOURS
//                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORKER_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )

            stopOneTimeService(context)
        }.logOnException()
    }

    fun startOneTimeService(context: Context) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<MindboxOneTimeEventWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag(ONE_TIME_WORKER_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()

            WorkManager
                .getInstance(context)
                .beginUniqueWork(
                    ONE_TIME_WORKER_TAG,
                    ExistingWorkPolicy.KEEP,
                    request
                )
                .enqueue()

            stopPeriodicService(context)
        }.logOnException()
    }

    private fun stopOneTimeService(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelAllWorkByTag(ONE_TIME_WORKER_TAG)
        }.returnOnException { }
    }

    private fun stopPeriodicService(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORKER_TAG)
        }.returnOnException { }
    }
}

internal enum class WorkerType {
    ONE_TIME_WORKER, PERIODIC_WORKER
}