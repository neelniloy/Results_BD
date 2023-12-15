package com.braineer.nuresult

import android.R
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.braineer.nuresult.databinding.FragmentWebViewBinding
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class WebViewFragment : Fragment() {

    lateinit var binding: FragmentWebViewBinding
    private var printWeb: WebView? = null
    private var printJob: PrintJob? = null
    private var printBtnPressed = false
    private var dialog:AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  FragmentWebViewBinding.inflate(inflater, container, false)


        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        binding.webview.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        binding.webview.settings.databaseEnabled = true
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.setSupportZoom(true)
        binding.webview.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webview.settings.builtInZoomControls = true

        binding.webview.settings.setGeolocationEnabled(true)

        //setting swiperefreshlistener
        binding.swiperefreshlayout.setOnRefreshListener{
            binding.swiperefreshlayout.isRefreshing = true
            Handler().postDelayed({

                binding.savePdfBtn.visibility = View.GONE
                binding.swiperefreshlayout.isRefreshing = false
                binding.webview.reload()

            }, 1500)
        }

        binding.swiperefreshlayout.setColorSchemeColors(

            resources.getColor(R.color.holo_red_dark),
            resources.getColor(R.color.holo_blue_dark),
            resources.getColor(R.color.holo_orange_dark),
            resources.getColor(R.color.holo_green_dark),

        )


        binding.webview.webViewClient = object : WebViewClient() {

/*            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(webView: WebView?, url: String): Boolean {
                return if (url.contains(arguments?.getString("url").toString())*//* || arguments?.getString("type").toString()=="NU"*//*) {
                    false
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    activity!!.startActivity(intent)
                    findNavController().popBackStack()
                    true
                }
            }*/
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setMessage("মেইন সার্ভারের সমস্যার জন্য অনেক সময় সাইট লোড হতে সময় লাগে। সেক্ষেত্রে উপর থেকে টেনে সোয়াইপ করে বার বার রিফ্রেশ করুন।\n" +
                        "অথবা আপনার ইন্টারনেট কানেকশন চেক করুন।")
                builder.setCancelable(false)
                builder.setPositiveButton("ঠিক আছে") { dialog: DialogInterface?, which: Int ->
                    handler.proceed()
                    binding.webview.reload()
                }
                //                builder.setNegativeButton("exit", (dialog, which) -> {
    //                    handler.cancel();
    //                    finish();
    //                });
                dialog = builder.create()
                dialog!!.show()

            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView,
                errorCod: Int,
                description: String,
                failingUrl: String
            ) {
                binding.savePdfBtn.visibility = View.GONE
                val builder = AlertDialog.Builder(requireActivity())
                builder.setMessage("মেইন সার্ভারের সমস্যার জন্য অনেক সময় সাইট লোড হতে সময় লাগে। সেক্ষেত্রে উপর থেকে টেনে সোয়াইপ করে বার বার রিফ্রেশ করুন।\nঅথবা আপনার ইন্টারনেট কানেকশন চেক করুন।")
                builder.setCancelable(true)
                builder.setPositiveButton(
                    "ঠিক আছে"
                ) { _: DialogInterface?, _: Int -> binding.webview.reload() }
                dialog = builder.create()
                dialog!!.show()
            }

            override fun onReceivedHttpError(
                view: WebView?, request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

        }

        arguments?.getString("url")?.let { binding.webview.loadUrl(it) }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }


        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                binding.progress.progress = progress
                if (progress > 99) {
                    printWeb = binding.webview
                    binding.progress.visibility = View.GONE
                    binding.savePdfBtn.visibility = View.VISIBLE

                    (activity as? MainActivity)?.supportActionBar?.title = view.title



                } else if (progress in 1..89) {

                        binding.progress.visibility = View.VISIBLE
                        binding.savePdfBtn.visibility = View.GONE

                }
            }
        }


        // setting clickListener for Save Pdf Button
        binding.savePdfBtn.setOnClickListener(View.OnClickListener {
            if (printWeb != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Calling createWebPrintJob()
                    PrintTheWebPage(printWeb!!)
                } else {
                    // Showing Toast message to user
                    Snackbar.make(
                        requireActivity().findViewById<View>(R.id.content),
                        "Not available for device below Android LOLLIPOP",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Showing Toast message to user
                Snackbar.make(
                    requireActivity().findViewById(R.id.content),
                    "WebPage not fully loaded",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {

                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack();
                    } else {
                        findNavController().popBackStack()
                        binding.webview.stopLoading()
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        return binding.root
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun PrintTheWebPage(webView: WebView) {

        // set printBtnPressed true
        printBtnPressed = true

        // Creating  PrintManager instance
        val printManager = requireActivity()
            .getSystemService(Context.PRINT_SERVICE) as PrintManager?
        val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy_HH:mm:ss a")
        val cal = Calendar.getInstance()

        // setting the name of job
        val jobName =  "Results BD_" + dateFormat.format(cal.time)

        // Creating  PrintDocumentAdapter instance
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        assert(printManager != null)
        printJob = printManager!!.print(
            jobName, printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    override fun onResume() {
        super.onResume()
        if (printJob != null && printBtnPressed) {
            if (printJob!!.isCompleted()) {
                // Showing Toast Message
                Snackbar.make(
                    requireActivity().findViewById<View>(R.id.content),
                    "PDF Saved Successfully",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (printJob!!.isStarted()) {
                // Showing Toast Message
                Snackbar.make(requireActivity().findViewById<View>(R.id.content), "Started", Snackbar.LENGTH_SHORT)
                    .show()
            } else if (printJob!!.isBlocked()) {
                // Showing Toast Message
                Snackbar.make(requireActivity().findViewById<View>(R.id.content), "Blocked", Snackbar.LENGTH_SHORT)
                    .show()
            } else if (printJob!!.isCancelled()) {
                // Showing Toast Message
                Snackbar.make(requireActivity().findViewById<View>(R.id.content), "Cancelled", Snackbar.LENGTH_SHORT)
                    .show()
            } else if (printJob!!.isFailed()) {
                // Showing Toast Message
                Snackbar.make(requireActivity().findViewById<View>(R.id.content), "Failed", Snackbar.LENGTH_SHORT)
                    .show()
            } else if (printJob!!.isQueued()) {
                // Showing Toast Message
                Snackbar.make(requireActivity().findViewById<View>(R.id.content), "Queued", Snackbar.LENGTH_SHORT)
                    .show()
            }
            // set printBtnPressed false
            printBtnPressed = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (dialog!=null){
            dialog!!.dismiss()
        }
    }
}