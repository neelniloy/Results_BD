package com.braineer.nuresult.model

import android.content.Context
import android.util.Log
import com.braineer.nuresult.R
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

enum class HscGroup(val key: String) { SCIENCE("science"), BUSINESS("business"), HUMANITIES("humanities") }

/** One admission track, e.g. "GST A Unit". Null minimums mean "no GPA rule". */
data class AdmissionTrack(
    val unit: String,
    val groups: List<String>,
    val minSsc: Double?,
    val minHsc: Double?,
    val minTotal: Double?,
    /** Rule counts GPA without the 4th subject, which the user doesn't enter. */
    val without4th: Boolean,
    /** Eligibility also depends on subject grades the user doesn't enter. */
    val subjectCheck: Boolean,
    val note: String,
)

data class AdmissionProgram(val name: String, val url: String, val tracks: List<AdmissionTrack>)

data class AdmissionRuleSet(val session: String, val programs: List<AdmissionProgram>)

enum class EligibilityStatus { ELIGIBLE, CHECK, NOT_ELIGIBLE }

data class EligibilityResult(
    val program: AdmissionProgram,
    val track: AdmissionTrack,
    val status: EligibilityStatus,
    /** Failed rules for NOT_ELIGIBLE, or what to verify for CHECK. */
    val reasons: List<String>,
)

/**
 * Admission GPA requirements, editable without an app update: a copy is bundled
 * in res/raw and a newer one is fetched from the config repo on jsDelivr.
 */
object AdmissionRules {

    private const val TAG = "AdmissionRules"
    private const val CONFIG_URL =
        "https://cdn.jsdelivr.net/gh/neelniloy/resultsbd-config@main/admission_rules.json"
    private const val CACHE_FILE = "admission_rules.json"
    private const val TIMEOUT_MS = 10_000

    /** Downloads the latest rules in the background; used on the next load(). */
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        Executors.newSingleThreadExecutor().execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(CONFIG_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@execute
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                parse(text) // only cache files that parse
                File(appContext.filesDir, CACHE_FILE).writeText(text)
            } catch (e: Exception) {
                Log.w(TAG, "Rules fetch failed, using cached/bundled rules", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun load(context: Context): AdmissionRuleSet {
        val cached = File(context.filesDir, CACHE_FILE)
        if (cached.exists()) {
            try {
                return parse(cached.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Cached rules invalid, using bundled rules", e)
            }
        }
        val bundled = context.resources.openRawResource(R.raw.admission_rules)
            .bufferedReader().use { it.readText() }
        return parse(bundled)
    }

    fun evaluate(rules: AdmissionRuleSet, group: HscGroup, ssc: Double, hsc: Double): List<EligibilityResult> {
        return rules.programs.flatMap { program ->
            program.tracks
                .filter { group.key in it.groups }
                .map { track -> evaluateTrack(program, track, ssc, hsc) }
        }.sortedBy { it.status.ordinal }
    }

    private fun evaluateTrack(program: AdmissionProgram, track: AdmissionTrack, ssc: Double, hsc: Double): EligibilityResult {
        val fails = mutableListOf<String>()
        track.minSsc?.let { if (ssc < it) fails += "Needs SSC GPA ${fmt(it)} (you have ${fmt(ssc)})" }
        track.minHsc?.let { if (hsc < it) fails += "Needs HSC GPA ${fmt(it)} (you have ${fmt(hsc)})" }
        track.minTotal?.let { if (ssc + hsc < it) fails += "Needs total GPA ${fmt(it)} (you have ${fmt(ssc + hsc)})" }
        if (fails.isNotEmpty()) return EligibilityResult(program, track, EligibilityStatus.NOT_ELIGIBLE, fails)

        // GPA without the 4th subject can only be lower, so passing with it is not proof
        val checks = mutableListOf<String>()
        if (track.without4th) checks += "Counts GPA without the 4th subject — check yours on the marksheet"
        if (track.subjectCheck) checks += "Also depends on subject grades — see the note"
        val status = if (checks.isEmpty()) EligibilityStatus.ELIGIBLE else EligibilityStatus.CHECK
        return EligibilityResult(program, track, status, checks)
    }

    private fun fmt(value: Double) = String.format(java.util.Locale.US, "%.2f", value)

    private fun parse(text: String): AdmissionRuleSet {
        val root = JSONObject(text)
        val programs = root.getJSONArray("programs")
        return AdmissionRuleSet(
            session = root.optString("session"),
            programs = (0 until programs.length()).map { i ->
                val p = programs.getJSONObject(i)
                val tracks = p.getJSONArray("tracks")
                AdmissionProgram(
                    name = p.getString("name"),
                    url = p.optString("url"),
                    tracks = (0 until tracks.length()).map { j ->
                        val t = tracks.getJSONObject(j)
                        val groups = t.getJSONArray("groups")
                        AdmissionTrack(
                            unit = t.getString("unit"),
                            groups = (0 until groups.length()).map { groups.getString(it) },
                            minSsc = t.optDoubleOrNull("minSsc"),
                            minHsc = t.optDoubleOrNull("minHsc"),
                            minTotal = t.optDoubleOrNull("minTotal"),
                            without4th = t.optBoolean("without4th"),
                            subjectCheck = t.optBoolean("subjectCheck"),
                            note = t.optString("note"),
                        )
                    }
                )
            }
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String) = if (has(key) && !isNull(key)) getDouble(key) else null
}
