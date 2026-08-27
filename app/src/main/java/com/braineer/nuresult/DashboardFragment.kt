package com.braineer.nuresult

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.braineer.nuresult.adapter.DashboardAdapter
import com.braineer.nuresult.ads.AdManager
import com.braineer.nuresult.databinding.FragmentDashboardBinding
import com.braineer.nuresult.model.UrlModel
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.FirebaseFirestore

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private var websiteurl: UrlModel = UrlModel()
    private var adView: AdView? = null
    private var initialLayoutComplete = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        val reference = FirebaseFirestore.getInstance()
            .collection("Website")
            .document("URL")

        reference.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                document.toObject(UrlModel::class.java)?.let {
                    websiteurl = it
                }
            }
        }.addOnFailureListener {
            // Falls back to default UrlModel() URLs
        }

        val adapter = DashboardAdapter({
            navigateToDashboardItemPage(it)
        }, { _, _ -> })

        val llm = LinearLayoutManager(requireActivity())
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerView.layoutManager = llm
        binding.recyclerView.adapter = adapter

        // Setup Banner Ad safely
        setupBannerAd()

        return binding.root
    }

    private fun setupBannerAd() {
        if (adView == null) {
            adView = AdView(requireContext())
            binding.bannerAd.removeAllViews()
            binding.bannerAd.addView(adView)

            binding.bannerAd.viewTreeObserver.addOnGlobalLayoutListener {
                if (!initialLayoutComplete && isAdded) {
                    initialLayoutComplete = true
                    adView?.let { ad ->
                        ad.adUnitId = getString(R.string.banner_ad_unit_id)
                        ad.setAdSize(adSize)
                        val extras = Bundle().apply {
                            putString("collapsible", "bottom")
                        }
                        val adRequest = AdRequest.Builder()
                            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                            .build()
                        ad.loadAd(adRequest)
                    }
                }
            }
        }
    }

    private fun navigateToDashboardItemPage(it: DashboardItemType) {
        // Show interstitial ad if ready with throttling
        activity?.let { act -> AdManager.showInterstitialAd(act) }

        when (it) {
            DashboardItemType.PSC -> {
                val url = websiteurl.psc ?: "http://www.educationboardresults.gov.bd/"
                val bundle = bundleOf("url" to url, "type" to "PSC")
                findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
            }
            DashboardItemType.SSC -> {
                val url = websiteurl.ssc ?: "http://www.educationboardresults.gov.bd/"
                val bundle = bundleOf("url" to url, "type" to "SSC")
                findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
            }
            DashboardItemType.OPEN -> {
                val url = websiteurl.open ?: "https://www.bou.ac.bd/result"
                val bundle = bundleOf("url" to url, "type" to "OPEN")
                findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
            }
            DashboardItemType.NU -> {
                val url = websiteurl.nu ?: "http://results.nu.ac.bd/"
                val bundle = bundleOf("url" to url, "type" to "NU")
                findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
            }
            DashboardItemType.ABOUT -> {
                findNavController().navigate(R.id.aboutFragment)
            }
        }
    }

    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val density = displayMetrics.density
            var adWidthPixels = binding.bannerAd.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = displayMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireContext(), adWidth)
        }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adView?.destroy()
        adView = null
    }
}