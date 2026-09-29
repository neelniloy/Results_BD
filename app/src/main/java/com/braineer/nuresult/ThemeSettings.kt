package com.braineer.nuresult

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** User's light/dark choice, persisted and applied app-wide via AppCompatDelegate. */
object ThemeSettings {

    private const val PREFS_NAME = "settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    // Order matches R.array.theme_options
    private val modes = intArrayOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES
    )

    /** Call from Application.onCreate so the theme is right before any Activity draws. */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(savedMode(context))
    }

    fun showPicker(context: Context) {
        val checked = modes.indexOf(savedMode(context)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(R.array.theme_options, checked) { dialog, which ->
                dialog.dismiss()
                val mode = modes[which]
                prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
            }
            .show()
    }

    private fun savedMode(context: Context) =
        prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
