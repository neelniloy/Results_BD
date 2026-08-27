package com.braineer.nuresult

import android.app.Application
import com.braineer.nuresult.ads.AdManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdManager with your actual ad unit ID
        AdManager.initializeInterstitialAd(this, getString(R.string.interstitial_ad_unit_id))

    }
}