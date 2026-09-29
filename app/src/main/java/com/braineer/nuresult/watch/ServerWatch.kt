package com.braineer.nuresult.watch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.braineer.nuresult.MainActivity
import com.braineer.nuresult.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * "Notify me when the server is back": checks an overloaded result site in the
 * background and posts a notification once it responds.
 *
 * Checks are deliberately sparse (every ~5 minutes with jitter, giving up after
 * [MAX_WATCH_MS]) so that many users watching at once don't add meaningful load
 * to the government server they are waiting on.
 */
object ServerWatch {

    private const val TAG = "ServerWatch"
    private const val CHANNEL_ID = "server_watch"
    private const val CHECK_INTERVAL_MIN = 5L
    private const val MAX_JITTER_SEC = 60
    private const val MAX_WATCH_MS = 3 * 60 * 60 * 1000L // 3 hours
    private const val TIMEOUT_MS = 15_000

    internal const val KEY_URL = "url"
    internal const val KEY_TYPE = "type"
    internal const val KEY_LABEL = "label"
    internal const val KEY_STARTED_AT = "started_at"

    /** Starts (or restarts) watching [url]; one watch per exam [type]. */
    fun start(context: Context, type: String, label: String, url: String) {
        val data = workDataOf(
            KEY_URL to url,
            KEY_TYPE to type,
            KEY_LABEL to label,
            KEY_STARTED_AT to System.currentTimeMillis()
        )
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(type), ExistingWorkPolicy.REPLACE, checkRequest(data))
    }

    internal fun scheduleNext(context: Context, type: String, data: androidx.work.Data) {
        // APPEND_OR_REPLACE queues after the currently running check instead of cancelling it
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(type), ExistingWorkPolicy.APPEND_OR_REPLACE, checkRequest(data))
    }

    private fun checkRequest(data: androidx.work.Data) =
        OneTimeWorkRequestBuilder<ServerWatchWorker>()
            .setInputData(data)
            .setInitialDelay(
                TimeUnit.MINUTES.toSeconds(CHECK_INTERVAL_MIN) + Random.nextInt(MAX_JITTER_SEC),
                TimeUnit.SECONDS
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

    private fun workName(type: String) = "server_watch_$type"

    internal fun isExpired(startedAt: Long) = System.currentTimeMillis() - startedAt > MAX_WATCH_MS

    /** True when the site answers with a non-error status within the timeout. */
    internal suspend fun isServerUp(url: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
            }
            connection.responseCode in 200..399
        } catch (e: Exception) {
            Log.d(TAG, "Still down: $url (${e.javaClass.simpleName})")
            false
        } finally {
            connection?.disconnect()
        }
    }

    internal fun notifyServerUp(context: Context, type: String, label: String, url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)
        val openResult = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.webViewFragment)
            .setArguments(bundleOf("url" to url, "type" to type))
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.watch_up_title))
            .setContentText(context.getString(R.string.watch_up_text, label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openResult)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(workName(type).hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.watch_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class ServerWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(ServerWatch.KEY_URL) ?: return Result.failure()
        val type = inputData.getString(ServerWatch.KEY_TYPE) ?: return Result.failure()
        val label = inputData.getString(ServerWatch.KEY_LABEL).orEmpty()
        val startedAt = inputData.getLong(ServerWatch.KEY_STARTED_AT, 0L)

        when {
            ServerWatch.isServerUp(url) -> ServerWatch.notifyServerUp(applicationContext, type, label, url)
            ServerWatch.isExpired(startedAt) -> Unit // give up quietly
            else -> ServerWatch.scheduleNext(applicationContext, type, inputData)
        }
        return Result.success()
    }
}
