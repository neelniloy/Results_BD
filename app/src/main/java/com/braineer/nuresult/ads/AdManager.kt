package com.niloythings.lanstreamer.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var adUnitId: String? = null
    private var adLoadTimer: Handler = Handler(Looper.getMainLooper())
    private var adLoadRunnable: Runnable? = null

    fun initializeInterstitialAd(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId

        MobileAds.initialize(context)

        adLoadRunnable = object : Runnable {
            override fun run() {
                if (interstitialAd == null) {
                    loadInterstitialAd(context, AdManager.adUnitId ?: "")
                    Log.d("ADS", "Start Ad Loading - ${System.currentTimeMillis()}")
                }
                adLoadTimer.postDelayed(this, 150 * 1000L)
            }
        }
        adLoadRunnable?.let { adLoadTimer.postDelayed(it, 30 * 1000L) }


    }

    private fun loadInterstitialAd(context: Context, adUnitId: String) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", adError.toString())
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("ADS", "Ad was loaded.")
                interstitialAd = ad
            }
        })
    }
    fun cancelAdLoading() {
        adLoadRunnable?.let {
            adLoadTimer.removeCallbacks(it)
            Log.d("ADS", "Cancel Ad Loading.")
        }
    }

    fun showInterstitialAd(activity: Activity) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd = null
            cancelAdLoading()
            adLoadRunnable?.let { adLoadTimer.postDelayed(it, 150 * 1000L) }
        } else {
            Log.d("ADS", "The interstitial ad wasn't ready yet.")
        }
    }
}
