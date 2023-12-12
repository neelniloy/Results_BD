package com.braineer.nuresult

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.braineer.nuresult.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var consentInformation: ConsentInformation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        setSupportActionBar(binding.mToolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


        //Consent Form
        val params = ConsentRequestParameters
            .Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity
                ) { loadAndShowError ->
                    // Consent gathering failed.
                    if (loadAndShowError != null) {
                        Log.w(
                            "TAG", String.format(
                                "%s: %s",
                                loadAndShowError.errorCode,
                                loadAndShowError.message
                            )
                        )
                    }

                    // Consent has been gathered.
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                Log.w(
                    "TAG", String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
            })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return true
    }

    override fun onBackPressed() {

        if (navController.currentDestination!!.id == R.id.dashboardFragment) {
            customExitDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun customExitDialog() {
        // creating custom dialog
        val dialog = Dialog(this@MainActivity)

        // setting content view to dialog
        dialog.setContentView(R.layout.custom_exit_dialog)

        // getting reference id
        val dialogButtonYes = dialog.findViewById<TextView>(R.id.textViewYes)
        val dialogButtonNo = dialog.findViewById<TextView>(R.id.textViewNo)
        val exitAd = dialog.findViewById<AdView>(R.id.exit_banner_ad)

        //ad
        val adRequest = AdRequest.Builder().build()
        exitAd.loadAd(adRequest)

        exitAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Ad has successfully loaded.
                exitAd.isVisible = true
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                // Ad failed to load.
                exitAd.isVisible = false
            }

            override fun onAdOpened() {
                // Ad has been clicked and opened.
            }

            override fun onAdClicked() {
                // User clicked on the ad.
            }

            override fun onAdClosed() {
                // Ad has been closed by the user.
            }
        }

        // click listener for No
        dialogButtonNo.setOnClickListener { v: View? ->
            //dismiss the dialog
            dialog.dismiss()
        }

        // click listener for Yes
        dialogButtonYes.setOnClickListener { v: View? ->
            // dismiss the dialog
            // and exit the exit
            dialog.dismiss()
            finish()
        }

        // show the exit dialog
        dialog.show()
    }
}