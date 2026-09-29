package com.braineer.nuresult

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.braineer.nuresult.databinding.FragmentWebViewBinding
import com.braineer.nuresult.watch.ServerWatch
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    // Hides "Save PDF" while an error page is shown instead of a result
    private var loadFailed = false
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
            // LOAD_DEFAULT revalidates with the server; LOAD_CACHE_ELSE_NETWORK showed stale
            // result pages (even offline) instead of fresh results or an error
            cacheMode = WebSettings.LOAD_DEFAULT
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

        binding.swiperefreshlayout.setColorSchemeColors(
            MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
        )
        binding.swiperefreshlayout.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerHigh)
        )
    }

    private fun setupClients() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadFailed = false
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel() // Strict adherence to Google Play Security policy
                showErrorDialog(getString(R.string.error_ssl))
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                binding.savePdfBtn.visibility = View.GONE
                showErrorDialog(getString(R.string.error_network), offerWatch = true)
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
                        getString(R.string.error_http, statusCode),
                        // Overload/outage codes only; a 404 won't fix itself by waiting
                        offerWatch = statusCode >= 500 || statusCode == 429
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
                        b.savePdfBtn.visibility = if (loadFailed) View.GONE else View.VISIBLE
                        (activity as? MainActivity)?.supportActionBar?.title = view.title
                    } else if (progress in 1..89) {
                        b.progress.visibility = View.VISIBLE
                        b.savePdfBtn.visibility = View.GONE
                    }
                }
            }
        }
    }

    /** [offerWatch] adds "notify me when it's back" for server-side failures. */
    private fun showErrorDialog(message: String, offerWatch: Boolean = false) {
        loadFailed = true
        _binding?.savePdfBtn?.visibility = View.GONE
        if (!isAdded || isDetached) return
        dialog?.dismiss()
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.error_retry) { _, _ ->
                binding.webview.reload()
            }
            .setNegativeButton(R.string.error_back) { _, _ ->
                findNavController().popBackStack()
            }
        if (offerWatch) {
            builder.setNeutralButton(R.string.watch_button) { _, _ -> requestServerWatch() }
        }
        dialog = builder.create()
        dialog?.show()
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startServerWatch() else showSnackbar(getString(R.string.watch_permission_denied))
        }

    private fun requestServerWatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startServerWatch()
        }
    }

    private fun startServerWatch() {
        val url = arguments?.getString("url") ?: return
        val type = arguments?.getString("type") ?: return
        val label = dashboardItemList.firstOrNull { it.type.name == type }
            ?.let { getString(it.title) } ?: type
        ServerWatch.start(requireContext(), type, label, url)
        showSnackbar(getString(R.string.watch_started, label))
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
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