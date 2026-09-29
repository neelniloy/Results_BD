package com.braineer.nuresult

import android.app.Application
import com.braineer.nuresult.ads.AdManager
import com.braineer.nuresult.model.ResultLinks

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeSettings.apply(this)
        // Initialize AdManager with your actual ad unit ID
        AdManager.initializeInterstitialAd(this, getString(R.string.interstitial_ad_unit_id))
        // Fetch result site URLs early so they are ready before the user taps a card
        ResultLinks.init(this)

    }
}