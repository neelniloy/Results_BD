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
import com.braineer.nuresult.ads.BannerAds
import com.braineer.nuresult.databinding.FragmentDashboardBinding
import com.braineer.nuresult.model.ResultLinks

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = DashboardAdapter { navigateToDashboardItemPage(it) }
        binding.eligibilityCard.setOnClickListener {
            // Natural transition into a tool; shares the interstitial frequency cap
            activity?.let { act -> AdManager.showInterstitialAd(act) }
            findNavController().navigate(R.id.action_dashboardFragment_to_eligibilityFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Collapsible banners open expanded over the exam cards, so only use one on the
        // first dashboard visit per app session and a regular banner when returning
        BannerAds.attach(binding.bannerAd, viewLifecycleOwner, collapsible = !collapsibleShown)
        collapsibleShown = true

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_dashboard, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_theme -> ThemeSettings.showPicker(requireContext())
                    R.id.action_about -> findNavController().navigate(R.id.action_dashboardFragment_to_aboutFragment)
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun navigateToDashboardItemPage(it: DashboardItemType) {
        // Show interstitial ad if ready with throttling
        activity?.let { act -> AdManager.showInterstitialAd(act) }

        val bundle = bundleOf("url" to ResultLinks.urlFor(it), "type" to it.name)
        findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
    }

    companion object {
        private var collapsibleShown = false
    }
}
