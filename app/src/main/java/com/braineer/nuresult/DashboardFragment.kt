package com.braineer.nuresult

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.braineer.nuresult.adapter.DashboardAdapter
import com.braineer.nuresult.databinding.FragmentDashboardBinding
import com.braineer.nuresult.model.UrlModel
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.niloythings.lanstreamer.ads.AdManager


class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private var websiteurl:UrlModel? = null
    private lateinit var adView: AdView
    private var initialLayoutComplete = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  FragmentDashboardBinding.inflate(inflater, container, false)


        //Initiate Ad View
        adView = AdView(requireContext())

        val reference = FirebaseFirestore.getInstance()
            .collection("Website")
            .document("URL")

        reference.get().addOnSuccessListener { document ->
            if (document != null) {
                websiteurl = document.toObject(UrlModel::class.java)!!
            }
        }

        val adapter = DashboardAdapter ({

            navigateToDashboardItemPage(it)

        },{item,position->


        })

        val llm = LinearLayoutManager(requireActivity())
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerView.layoutManager = llm
        binding.recyclerView.adapter = adapter


        //ads
        binding.bannerAd.addView(adView)

        binding.bannerAd.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                adView.adUnitId = getString(R.string.banner_ad_unit_id)
                adView.setAdSize(adSize)
                val extras = Bundle()
                extras.putString("collapsible", "bottom")
                val adRequest = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
                adView.loadAd(adRequest)
            }
        }


        return binding.root
    }

    private fun navigateToDashboardItemPage(it: DashboardItemType) {
        when(it) {

            DashboardItemType.PSC -> {

                if (websiteurl!=null){
                    val bundle = bundleOf(
                        "url" to websiteurl!!.psc,
                        "type" to "PSC"
                    )
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.SSC -> {

                if (websiteurl!=null){
                    val bundle = bundleOf(
                        "url" to websiteurl!!.ssc,
                        "type" to "SSC"
                    )
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.OPEN -> {

                if (websiteurl!=null){
                    val bundle = bundleOf(
                        "url" to websiteurl!!.open,
                        "type" to "OPEN"
                    )
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.NU -> {

                if (websiteurl!=null){
                    val bundle = bundleOf(
                        "url" to websiteurl!!.nu,
                        "type" to "NU"
                    )
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }


            DashboardItemType.ABOUT -> {

                findNavController().navigate(R.id.aboutFragment)
            }


        }
    }

    private val adSize: AdSize
        get() {
            val display = requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = binding.bannerAd.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireActivity(), adWidth)
        }
    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        AdManager.showInterstitialAd(requireActivity())
        adView.resume()
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adView.destroy()
    }

}