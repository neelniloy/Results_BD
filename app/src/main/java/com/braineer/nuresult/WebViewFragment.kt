package com.braineer.nuresult

import android.annotation.SuppressLint
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.braineer.nuresult.databinding.FragmentWebViewBinding
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class WebViewFragment : Fragment() {

    private var _binding: FragmentWebViewBinding? = null
    private val binding get() = _binding!!

    private var printWeb: WebView? = null
    private var printJob: PrintJob? = null
    private var printBtnPressed = false
    private var dialog: AlertDialog? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebViewBinding.inflate(inflater, container, false)

        setupWebViewSettings()
        setupSwipeRefresh()
        setupClients()
        setupPdfPrintButton()
        setupBackNavigation()

        arguments?.getString("url")?.let {
            binding.webview.loadUrl(it)
        }

        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewSettings() {
        binding.webview.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swiperefreshlayout.setOnRefreshListener {
            binding.swiperefreshlayout.isRefreshing = true
            binding.savePdfBtn.visibility = View.GONE
            mainHandler.postDelayed({
                _binding?.let {
                    it.swiperefreshlayout.isRefreshing = false
                    it.webview.reload()
                }
            }, 1500)
        }

        val ctx = requireContext()
        binding.swiperefreshlayout.setColorSchemeColors(
            ContextCompat.getColor(ctx, android.R.color.holo_red_dark),
            ContextCompat.getColor(ctx, android.R.color.holo_blue_dark),
            ContextCompat.getColor(ctx, android.R.color.holo_orange_dark),
            ContextCompat.getColor(ctx, android.R.color.holo_green_dark)
        )
    }

    private fun setupClients() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel() // Strict adherence to Google Play Security policy
                showErrorDialog(
                    "নিরাপদ কানেকশন স্থাপন করা সম্ভব হয়নি। অনুগ্রহ করে আপনার ইন্টারনেট কানেকশন ও ফোনের তারিখ/সময় চেক করুন।"
                )
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                binding.savePdfBtn.visibility = View.GONE
                showErrorDialog(
                    "মেইন সার্ভারের সমস্যার জন্য অনেক সময় সাইট লোড হতে সময় লাগে। উপর থেকে টেনে সোয়াইপ করে আবার চেষ্টা করুন।"
                )
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val statusCode = errorResponse?.statusCode ?: 200
                if (request?.isForMainFrame == true && statusCode >= 400) {
                    binding.savePdfBtn.visibility = View.GONE
                    showErrorDialog(
                        "রেজাল্ট সার্ভারটি বর্তমানে অনুপলব্ধ বা অতিরিক্ত ট্রাফিকের কারণে ব্যস্ত আছে (HTTP $statusCode)। অনুগ্রহ করে কিছুক্ষণ পর আবার চেষ্টা করুন।"
                    )
                }
            }
        }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }

            override fun onProgressChanged(view: WebView, progress: Int) {
                _binding?.let { b ->
                    b.progress.progress = progress
                    if (progress > 99) {
                        printWeb = b.webview
                        b.progress.visibility = View.GONE
                        b.savePdfBtn.visibility = View.VISIBLE
                        (activity as? MainActivity)?.supportActionBar?.title = view.title
                    } else if (progress in 1..89) {
                        b.progress.visibility = View.VISIBLE
                        b.savePdfBtn.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        if (!isAdded || isDetached) return
        dialog?.dismiss()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("আবার চেষ্টা করুন") { _, _ ->
                binding.webview.reload()
            }
            .setNegativeButton("ফিরে যান") { _, _ ->
                findNavController().popBackStack()
            }
            .create()
        dialog?.show()
    }

    private fun setupPdfPrintButton() {
        binding.savePdfBtn.setOnClickListener {
            val web = printWeb
            if (web != null) {
                printTheWebPage(web)
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "WebPage not fully loaded",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupBackNavigation() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (_binding != null && binding.webview.canGoBack()) {
                    binding.webview.goBack()
                } else {
                    _binding?.webview?.stopLoading()
                    findNavController().popBackStack()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun printTheWebPage(webView: WebView) {
        printBtnPressed = true
        val printManager = requireActivity().getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                "Print service not available on this device",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy_HH:mm:ss a", Locale.getDefault())
        val cal = Calendar.getInstance()
        val jobName = "Results BD_" + dateFormat.format(cal.time)
        val printAdapter = webView.createPrintDocumentAdapter(jobName)

        printJob = printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    override fun onResume() {
        super.onResume()
        if (printJob != null && printBtnPressed) {
            val job = printJob
            if (job != null) {
                val message = when {
                    job.isCompleted -> "PDF Saved Successfully"
                    job.isStarted -> "Started"
                    job.isBlocked -> "Blocked"
                    job.isCancelled -> "Cancelled"
                    job.isFailed -> "Failed"
                    job.isQueued -> "Queued"
                    else -> null
                }
                if (message != null) {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            printBtnPressed = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
        dialog = null
        mainHandler.removeCallbacksAndMessages(null)

        _binding?.let {
            it.webview.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
        _binding = null
    }
}