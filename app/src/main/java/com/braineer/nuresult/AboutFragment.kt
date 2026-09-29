package com.braineer.nuresult

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.braineer.nuresult.ads.BannerAds
import com.braineer.nuresult.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        binding.versionText.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        val openPortfolio = View.OnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://niloythings.pages.dev/")))
        }
        binding.maintainerCard.setOnClickListener(openPortfolio)
        binding.portfolio.setOnClickListener(openPortfolio)

        binding.supportGroup.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+JNQp4agxq2swNWQ1")))
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BannerAds.attach(binding.bannerAd, viewLifecycleOwner, collapsible = true)
    }
}
