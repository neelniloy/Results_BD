package com.braineer.nuresult

import android.app.Dialog
import android.os.Bundle
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.niloythings.lanstreamer.ads.AdManager


class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private var websiteurl:UrlModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  FragmentDashboardBinding.inflate(inflater, container, false)


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


        //ad
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        binding.bottomBannerAd.loadAd(adRequest)




        return binding.root
    }

    private fun navigateToDashboardItemPage(it: DashboardItemType) {
        when(it) {

            DashboardItemType.PSC -> {

                if (websiteurl!=null){
                    val bundle = bundleOf("url" to websiteurl!!.psc)
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.SSC -> {

                if (websiteurl!=null){
                    val bundle = bundleOf("url" to websiteurl!!.ssc)
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.OPEN -> {

                if (websiteurl!=null){
                    val bundle = bundleOf("url" to websiteurl!!.open)
                    findNavController().navigate(R.id.action_dashboardFragment_to_webViewFragment, bundle)
                }else{
                    Toast.makeText(requireActivity(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
                }

            }

            DashboardItemType.NU -> {

                if (websiteurl!=null){
                    val bundle = bundleOf("url" to websiteurl!!.nu)
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

    override fun onResume() {
        super.onResume()
        AdManager.showInterstitialAd(requireActivity())
    }

}