package com.braineer.nuresult

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.braineer.nuresult.ads.BannerAds
import com.braineer.nuresult.databinding.FragmentEligibilityBinding
import com.braineer.nuresult.databinding.ItemEligibilityResultBinding
import com.braineer.nuresult.model.AdmissionRuleSet
import com.braineer.nuresult.model.AdmissionRules
import com.braineer.nuresult.model.EligibilityResult
import com.braineer.nuresult.model.EligibilityStatus
import com.braineer.nuresult.model.HscGroup
import com.google.android.material.textfield.TextInputLayout

class EligibilityFragment : Fragment() {

    private lateinit var binding: FragmentEligibilityBinding
    private lateinit var rules: AdmissionRuleSet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEligibilityBinding.inflate(inflater, container, false)
        rules = AdmissionRules.load(requireContext())
        binding.sessionText.text = getString(R.string.elig_session, rules.session)

        binding.checkButton.setOnClickListener { check() }
        binding.hscInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                check()
                true
            } else false
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BannerAds.attach(binding.bannerAd, viewLifecycleOwner, collapsible = false)
    }

    private fun check() {
        val ssc = readGpa(binding.sscLayout)
        val hsc = readGpa(binding.hscLayout)
        if (ssc == null || hsc == null) return

        val group = when (binding.groupToggle.checkedButtonId) {
            R.id.groupBusiness -> HscGroup.BUSINESS
            R.id.groupHumanities -> HscGroup.HUMANITIES
            else -> HscGroup.SCIENCE
        }
        hideKeyboard()
        showResults(AdmissionRules.evaluate(rules, group, ssc, hsc))
    }

    /** Returns the GPA, or null after showing an error on the field. */
    private fun readGpa(layout: TextInputLayout): Double? {
        val value = layout.editText?.text?.toString()?.trim()?.toDoubleOrNull()
        return if (value == null || value < 1.0 || value > 5.0) {
            layout.error = getString(R.string.elig_gpa_error)
            null
        } else {
            layout.error = null
            value
        }
    }

    private fun showResults(results: List<EligibilityResult>) {
        val container = binding.resultsContainer
        container.removeAllViews()
        results.forEach { result ->
            val item = ItemEligibilityResultBinding.inflate(layoutInflater, container, false)
            item.programName.text = result.program.name
            item.unitName.text = result.track.unit
            bindStatus(item, result.status)

            item.reasons.visibility = if (result.reasons.isEmpty()) View.GONE else View.VISIBLE
            item.reasons.text = result.reasons.joinToString("\n") { "• $it" }
            item.note.visibility = if (result.track.note.isBlank()) View.GONE else View.VISIBLE
            item.note.text = result.track.note

            item.officialSite.visibility = if (result.program.url.isBlank()) View.GONE else View.VISIBLE
            item.officialSite.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.program.url)))
            }
            container.addView(item.root)
        }
        binding.summaryText.visibility = View.VISIBLE
        binding.summaryText.text = summary(results)
        binding.disclaimer.visibility = View.VISIBLE
        binding.scroll.post { binding.scroll.smoothScrollTo(0, binding.summaryText.top) }
    }

    private fun summary(results: List<EligibilityResult>): String {
        val counts = results.groupingBy { it.status }.eachCount()
        return listOf(
            EligibilityStatus.ELIGIBLE to R.string.elig_sum_eligible,
            EligibilityStatus.CHECK to R.string.elig_sum_check,
            EligibilityStatus.NOT_ELIGIBLE to R.string.elig_sum_not,
        ).mapNotNull { (status, res) -> counts[status]?.let { getString(res, it) } }
            .joinToString(" · ")
    }

    private fun bindStatus(item: ItemEligibilityResultBinding, status: EligibilityStatus) {
        val (label, bg, fg) = when (status) {
            EligibilityStatus.ELIGIBLE -> Triple(R.string.elig_status_eligible, R.color.status_ok_bg, R.color.status_ok_fg)
            EligibilityStatus.CHECK -> Triple(R.string.elig_status_check, R.color.status_check_bg, R.color.status_check_fg)
            EligibilityStatus.NOT_ELIGIBLE -> Triple(R.string.elig_status_not, R.color.status_no_bg, R.color.status_no_fg)
        }
        item.statusChip.setText(label)
        item.statusChip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bg))
        item.statusChip.setTextColor(ContextCompat.getColor(requireContext(), fg))
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.root.clearFocus()
    }
}
