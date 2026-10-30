package com.braineer.nuresult

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import com.braineer.nuresult.databinding.DialogSmsResultBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Board results by SMS through Teletalk (16222), the official fallback that keeps
 * working when every result website is overloaded on result day.
 *
 * Opens the user's SMS app with the message filled in; nothing is sent without them,
 * so the app needs no SMS permission.
 */
object SmsResult {

    const val NUMBER = "16222"

    val exams = listOf("HSC", "SSC", "Alim", "Dakhil")

    /** Boards by their SMS code (the first three letters of the board's name). */
    val boards = linkedMapOf(
        "DHA" to "Dhaka",
        "CHI" to "Chattogram",
        "COM" to "Cumilla",
        "RAJ" to "Rajshahi",
        "JES" to "Jashore",
        "BAR" to "Barishal",
        "SYL" to "Sylhet",
        "DIN" to "Dinajpur",
        "MYM" to "Mymensingh",
        "MAD" to "Madrasah",
        "TEC" to "Technical"
    )

    private const val MADRASAH = "MAD"

    /** e.g. "HSC DHA 123456 2026". Alim and Dakhil always go to the Madrasah board. */
    fun message(exam: String, board: String, roll: String, year: String): String {
        val code = if (exam == "Alim" || exam == "Dakhil") MADRASAH else board
        return "$exam $code ${roll.trim()} ${year.trim()}"
    }

    fun showDialog(context: Context) {
        val binding = DialogSmsResultBinding.inflate(LayoutInflater.from(context))
        val boardLabels = boards.map { (code, name) -> "$name ($code)" }
        val boardCodes = boards.keys.toList()

        binding.examInput.setSimpleItems(exams.toTypedArray())
        binding.examInput.setText(exams.first(), false)
        binding.boardInput.setSimpleItems(boardLabels.toTypedArray())
        binding.boardInput.setText(boardLabels.first(), false)
        binding.examInput.setOnItemClickListener { _, _, position, _ ->
            val madrasah = exams[position] == "Alim" || exams[position] == "Dakhil"
            if (madrasah) binding.boardInput.setText(boardLabels[boardCodes.indexOf(MADRASAH)], false)
            binding.boardLayout.isEnabled = !madrasah
        }
        binding.yearInput.setText(Calendar.getInstance().get(Calendar.YEAR).toString())

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.sms_title)
            .setView(binding.root)
            .setPositiveButton(R.string.sms_send, null) // set below so invalid input keeps the dialog open
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val roll = binding.rollInput.text?.toString().orEmpty().trim()
                val year = binding.yearInput.text?.toString().orEmpty().trim()
                binding.rollLayout.error = if (roll.isEmpty()) context.getString(R.string.sms_roll_required) else null
                binding.yearLayout.error = if (year.length != 4) context.getString(R.string.sms_year_invalid) else null
                if (roll.isEmpty() || year.length != 4) return@setOnClickListener

                val exam = binding.examInput.text.toString()
                val board = boardCodes[boardLabels.indexOf(binding.boardInput.text.toString()).coerceAtLeast(0)]
                if (openSmsApp(context, message(exam, board, roll, year))) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun openSmsApp(context: Context, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$NUMBER")).putExtra("sms_body", body)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.sms_no_app, body, NUMBER), Toast.LENGTH_LONG).show()
            false
        }
    }
}
