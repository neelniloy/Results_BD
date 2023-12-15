package com.braineer.nuresult

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.braineer.nuresult.databinding.FragmentAboutBinding
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.niloythings.lanstreamer.ads.AdManager


class AboutFragment : Fragment() {

    private lateinit var binding : FragmentAboutBinding
    private lateinit var adView: AdView
    private var initialLayoutComplete = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        //Initiate Ad View
        adView = AdView(requireContext())

        binding.logo.setOnClickListener {
            //Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show()
        }


        binding.facebook.setOnClickListener {
            val defaultBrowser = Intent(Intent.ACTION_VIEW)
            defaultBrowser.data = Uri.parse("https://www.facebook.com/neel.niloya/")
            startActivity(defaultBrowser)
        }

        binding.telegram.setOnClickListener {
            val defaultBrowser = Intent(Intent.ACTION_VIEW)
            defaultBrowser.data = Uri.parse("https://t.me/neelniloy")
            startActivity(defaultBrowser)
        }

        binding.github.setOnClickListener {
            val defaultBrowser = Intent(Intent.ACTION_VIEW)
            defaultBrowser.data = Uri.parse("https://github.com/neelniloy")
            startActivity(defaultBrowser)
        }

        binding.linkedin.setOnClickListener {
            val defaultBrowser = Intent(Intent.ACTION_VIEW)
            defaultBrowser.data = Uri.parse("https://www.linkedin.com/in/niloysarker/")
            startActivity(defaultBrowser)
        }

        binding.youtube.setOnClickListener {
            val defaultBrowser = Intent(Intent.ACTION_VIEW)
            defaultBrowser.data = Uri.parse("https://youtube.com/@niloythings")
            startActivity(defaultBrowser)
        }

        binding.cardRating.setOnClickListener {

            val uri: Uri = Uri.parse("market://details?id=${requireActivity().packageName}")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=${requireActivity().packageName}")))
            }
        }

        //ads
        binding.bannerAd.addView(adView)

        binding.bannerAd.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                adView.adUnitId = getString(R.string.banner_ad_unit_id)
                adView.setAdSize(adSize)
                val extras = Bundle()
                extras.putString("collapsible", "bottom")
                val adRequest = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
                adView.loadAd(adRequest)
            }
        }

        return binding.root
    }

    private val adSize: AdSize
        get() {
            val display = requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = binding.bannerAd.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireActivity(), adWidth)
        }
    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        adView.resume()
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adView.destroy()
    }

}