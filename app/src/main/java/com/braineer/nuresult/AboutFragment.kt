package com.braineer.nuresult

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.braineer.nuresult.databinding.FragmentAboutBinding
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding
    private var adView: AdView? = null
    private var initialLayoutComplete = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        binding.facebook.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/niloythings/"))
            startActivity(intent)
        }

        binding.telegram.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/niloythings"))
            startActivity(intent)
        }

        binding.github.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/neelniloy"))
            startActivity(intent)
        }

        binding.linkedin.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/niloysarker/"))
            startActivity(intent)
        }

        binding.youtube.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@niloythings"))
            startActivity(intent)
        }

        binding.cardRating.setOnClickListener {
            val uri = Uri.parse("market://details?id=${requireActivity().packageName}")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireActivity().packageName}")))
            }
        }

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