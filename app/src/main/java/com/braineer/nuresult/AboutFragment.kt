package com.braineer.nuresult

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.braineer.nuresult.databinding.FragmentAboutBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds


class AboutFragment : Fragment() {

    private lateinit var binding : FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAboutBinding.inflate(inflater, container, false)

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


        //ad
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        binding.bottomBannerAd.loadAd(adRequest)

        return binding.root
    }

}