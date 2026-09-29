package com.braineer.nuresult.ads

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.braineer.nuresult.R
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Loads an adaptive banner into [container] and ties the AdView to [owner]'s
 * lifecycle (pass a Fragment's viewLifecycleOwner). A new banner is loaded every
 * time the view is recreated, e.g. when navigating back to a screen.
 * [collapsible] requests a collapsible banner, which opens expanded over the content.
 */
object BannerAds {

    private const val TAG = "ADS"

    fun attach(container: FrameLayout, owner: LifecycleOwner, collapsible: Boolean) {
        val context = container.context
        val adView = AdView(context)
        container.removeAllViews()
        container.addView(adView)

        // Adaptive size needs the real container width, known only after layout
        container.doOnLayout {
            val density = context.resources.displayMetrics.density
            val widthPx = container.width.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
            adView.adUnitId = context.getString(R.string.banner_ad_unit_id)
            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (widthPx / density).toInt())
            )
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Banner failed to load: ${error.code} ${error.message}")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Banner loaded")
                }
            }
            val request = AdRequest.Builder()
            if (collapsible) {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                request.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }
            adView.loadAd(request.build())
        }

        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = adView.resume()
            override fun onPause(owner: LifecycleOwner) = adView.pause()
            override fun onDestroy(owner: LifecycleOwner) {
                container.removeView(adView)
                adView.destroy()
            }
        })
    }
}
