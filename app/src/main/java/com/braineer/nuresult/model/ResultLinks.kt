package com.braineer.nuresult.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.braineer.nuresult.DashboardItemType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Result website URLs, overridable by editing a static JSON file served from a CDN.
 * A static file has no per-read quota (unlike Firestore or Remote Config), so it
 * scales with any number of users. The last good copy is cached on device and the
 * bundled defaults are used until a fetch succeeds.
 *
 * Expected JSON (see config/result_links.json in the repo):
 * { "url_psc": "...", "url_ssc": "...", "url_nu": "...", "url_open": "..." }
 */
object ResultLinks {

    private const val TAG = "ResultLinks"
    private const val CONFIG_URL =
        "https://cdn.jsdelivr.net/gh/neelniloy/resultsbd-config@main/result_links.json"
    private const val PREFS_NAME = "result_links"
    private const val TIMEOUT_MS = 10_000

    const val KEY_PSC = "url_psc"
    const val KEY_SSC = "url_ssc"
    const val KEY_NU = "url_nu"
    const val KEY_OPEN = "url_open"

    private val defaults = mapOf(
        KEY_PSC to "https://www.educationboardresults.gov.bd/",
        KEY_SSC to "https://www.educationboardresults.gov.bd/",
        KEY_NU to "https://results.nu.ac.bd/",
        KEY_OPEN to "https://result.bou.ac.bd/"
    )

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Executors.newSingleThreadExecutor().execute { fetchRemote() }
    }

    private fun fetchRemote() {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(CONFIG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Config fetch failed: HTTP ${connection.responseCode}")
                return
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val editor = prefs.edit()
            for (key in defaults.keys) {
                val url = json.optString(key)
                if (url.startsWith("http")) editor.putString(key, url)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Config fetch failed, using cached/default URLs", e)
        } finally {
            connection?.disconnect()
        }
    }

    fun urlFor(type: DashboardItemType): String {
        val key = when (type) {
            DashboardItemType.PSC -> KEY_PSC
            DashboardItemType.SSC -> KEY_SSC
            DashboardItemType.NU -> KEY_NU
            DashboardItemType.OPEN -> KEY_OPEN
        }
        val cached = if (::prefs.isInitialized) prefs.getString(key, null) else null
        return cached ?: defaults.getValue(key)
    }
}
