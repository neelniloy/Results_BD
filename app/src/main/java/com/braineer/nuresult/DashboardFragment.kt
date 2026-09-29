package com.braineer.nuresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.braineer.nuresult.adapter.DashboardAdapter
import com.braineer.nuresult.ads.AdManager
import com.braineer.nuresult.databinding.FragmentDashboardBinding
import com.braineer.nuresult.model.ResultLinks
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private var adView: AdView? = null
    private var initialLayoutComplete = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = DashboardAdapter { navigateToDashboardItemPage(it) }

        // Setup Banner Ad safely
        setupBannerAd()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_dashboard, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId != R.id.action_about) return false
                findNavController().navigate(R.id.action_dashboardFragment_to_aboutFragment)
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

    private fun navigateToDashboardItemPage(it: DashboardItemType) {
        // Show interstitial ad if ready with throttling
        activity?.let { act -> AdManager.showInterstitialAd(act) }

        val bundle = bundleOf("url" to ResultLinks.urlFor(it), "type" to it.name)
        findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
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