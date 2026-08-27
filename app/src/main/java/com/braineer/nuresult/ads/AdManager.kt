package com.braineer.nuresult.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var adUnitId: String? = null
    private var appContext: Context? = null
    private var lastAdShownTime: Long = 0L
    private const val MIN_AD_INTERVAL_MS = 60 * 1000L // 60 seconds interval

    fun initializeInterstitialAd(context: Context, adUnitId: String) {
        this.appContext = context.applicationContext
        this.adUnitId = adUnitId

        MobileAds.initialize(context.applicationContext)
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val ctx = appContext ?: return
        val unitId = adUnitId ?: return
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(ctx, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ADS", "Interstitial load failed: ${adError.message}")
                interstitialAd = null
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("ADS", "Interstitial ad loaded successfully.")
                interstitialAd = ad
            }
        })
    }

    fun showInterstitialAd(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - lastAdShownTime < MIN_AD_INTERVAL_MS) {
            Log.d("ADS", "Interstitial throttled to prevent spamming user.")
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("ADS", "Interstitial dismissed.")
                    interstitialAd = null
                    lastAdShownTime = System.currentTimeMillis()
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("ADS", "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } else {
            Log.d("ADS", "The interstitial ad was not ready.")
            loadInterstitialAd()
        }
    }
}

