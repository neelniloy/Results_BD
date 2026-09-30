package com.braineer.nuresult.model

import android.content.Context
import android.util.Log
import com.braineer.nuresult.DashboardItemType
import com.braineer.nuresult.R
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Result website URLs per exam, as an ordered list of mirrors (most reliable first).
 * On result day the main site is often overloaded, so the result page can fail over
 * to, or let the user switch to, another mirror.
 *
 * Editable without an app update: a copy is bundled in res/raw and a newer one is
 * fetched from the config repo on jsDelivr (a static file, so no per-read quota).
 * Whichever copy has the later "updatedAt" wins.
 *
 * Format: { "updatedAt": "yyyy-MM-dd", "exams": { "ssc": ["https://...", ...], ... } }
 */
object ResultLinks {

    private const val TAG = "ResultLinks"
    private const val CONFIG_URL =
        "https://cdn.jsdelivr.net/gh/neelniloy/resultsbd-config@main/result_links.json"
    private const val CACHE_FILE = "result_links.json"
    private const val TIMEOUT_MS = 10_000

    private class LinkSet(val updatedAt: String, val exams: Map<String, List<String>>)

    private lateinit var appContext: Context
    private val bundled: LinkSet by lazy {
        parse(appContext.resources.openRawResource(R.raw.result_links).bufferedReader().use { it.readText() })
    }

    fun init(context: Context) {
        appContext = context.applicationContext
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
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parse(text) // only cache files that parse
            File(appContext.filesDir, CACHE_FILE).writeText(text)
        } catch (e: Exception) {
            Log.w(TAG, "Config fetch failed, using cached/bundled links", e)
        } finally {
            connection?.disconnect()
        }
    }

    /** All mirrors for [type], most reliable first. Never empty. */
    fun urlsFor(type: DashboardItemType): List<String> {
        val key = type.name.lowercase()
        return current().exams[key]?.takeIf { it.isNotEmpty() }
            ?: bundled.exams[key].orEmpty()
    }

    fun urlFor(type: DashboardItemType): String = urlsFor(type).first()

    private fun current(): LinkSet {
        val cached = try {
            val file = File(appContext.filesDir, CACHE_FILE)
            if (file.exists()) parse(file.readText()) else null
        } catch (e: Exception) {
            Log.w(TAG, "Cached links invalid, using bundled links", e)
            null
        }
        return if (cached != null && cached.updatedAt > bundled.updatedAt) cached else bundled
    }

    private fun parse(text: String): LinkSet {
        val root = JSONObject(text)
        val exams = root.getJSONObject("exams")
        val map = exams.keys().asSequence().associateWith { key ->
            val array = exams.getJSONArray(key)
            dedupe((0 until array.length()).map { array.getString(it).trim() })
        }
        return LinkSet(root.optString("updatedAt"), map)
    }

    /** Keeps valid http(s) URLs, dropping later entries that point to the same server. */
    internal fun dedupe(urls: List<String>): List<String> =
        urls.filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinctBy(::serverKey)

    /** True when [a] and [b] are the same server (see [serverKey]). */
    fun sameServer(a: String, b: String) = serverKey(a) == serverKey(b)

    /**
     * Identity of a mirror, ignoring differences that still reach the same page:
     * http vs https, a leading "www.", letter case in the host and a trailing slash.
     * The path and query still matter.
     */
    internal fun serverKey(url: String): String = try {
        val uri = java.net.URI(url.trim())
        val host = uri.host.orEmpty().lowercase().removePrefix("www.")
        val port = if (uri.port == -1 || uri.port == 80 || uri.port == 443) "" else ":${uri.port}"
        val path = uri.rawPath.orEmpty().trimEnd('/')
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        "$host$port$path$query"
    } catch (e: Exception) {
        url.trim().lowercase()
    }
}
