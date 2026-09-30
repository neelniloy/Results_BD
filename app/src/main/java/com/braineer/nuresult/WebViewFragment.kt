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
import com.braineer.nuresult.model.ResultLinks
import com.braineer.nuresult.watch.ServerWatch
import com.google.android.material.chip.Chip
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

    // Result site mirrors for this exam, most reliable first
    private var mirrors: List<String> = emptyList()
    private var mirrorIndex = 0
    // True until the current mirror's first page loads. Automatic failover only happens
    // then, so an error after the user submits their roll number never moves them silently.
    private var landingLoad = true
    private var failoverAttempts = 0
    private val landingTimeout = Runnable {
        if (landingLoad) handleLoadError(getString(R.string.error_network), offerWatch = true)
    }

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

        setupMirrors()

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
            // Fit desktop-only mirrors (e.g. the boards' IP servers) to the screen width;
            // pages with a mobile viewport tag are unaffected
            useWideViewPort = true
            loadWithOverviewMode = true
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

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (landingLoad && !loadFailed) {
                    // This mirror works; stop automatic switching from here on
                    landingLoad = false
                    failoverAttempts = 0
                    mainHandler.removeCallbacks(landingTimeout)
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel() // Strict adherence to Google Play Security policy
                handleLoadError(getString(R.string.error_ssl), offerWatch = false)
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                handleLoadError(getString(R.string.error_network), offerWatch = true)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val statusCode = errorResponse?.statusCode ?: 200
                if (request?.isForMainFrame == true && statusCode >= 400) {
                    handleLoadError(
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

    private fun setupMirrors() {
        val type = arguments?.getString("type")
        val requested = arguments?.getString("url")
        val known = DashboardItemType.entries.firstOrNull { it.name == type }
            ?.let { ResultLinks.urlsFor(it) }.orEmpty()
        // A deep link (e.g. from a server watch notification) may name a specific mirror
        mirrors = if (requested != null && known.none { ResultLinks.sameServer(it, requested) }) {
            listOf(requested) + known
        } else {
            known
        }
        if (mirrors.isEmpty()) return

        if (mirrors.size > 1) {
            binding.serverBar.visibility = View.VISIBLE
            mirrors.indices.forEach { i ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    tag = i
                    text = getString(R.string.server_label, i + 1)
                    isCheckable = true
                    setOnClickListener { loadMirror(i, userChoice = true) }
                }
                binding.serverChips.addView(chip)
            }
        }
        val start = requested?.let { r -> mirrors.indexOfFirst { ResultLinks.sameServer(it, r) } } ?: 0
        loadMirror(start.coerceAtLeast(0), userChoice = true)
    }

    private fun loadMirror(index: Int, userChoice: Boolean) {
        val b = _binding ?: return
        mirrorIndex = index
        landingLoad = true
        if (userChoice) failoverAttempts = 0
        dialog?.dismiss()
        b.serverChips.findViewWithTag<Chip>(index)?.let {
            b.serverChips.check(it.id)
            b.serverBar.post { b.serverBar.smoothScrollTo(it.left - it.width, 0) }
        }
        mainHandler.removeCallbacks(landingTimeout)
        mainHandler.postDelayed(landingTimeout, LANDING_TIMEOUT_MS)
        b.webview.loadUrl(mirrors[index])
    }

    /**
     * While a mirror's first page is loading, quietly moves on to the next mirror; once
     * every mirror has been tried (or after the page has loaded), shows the error dialog.
     */
    private fun handleLoadError(message: String, offerWatch: Boolean) {
        loadFailed = true
        _binding?.savePdfBtn?.visibility = View.GONE
        if (landingLoad && failoverAttempts < mirrors.size - 1) {
            failoverAttempts++
            val next = (mirrorIndex + 1) % mirrors.size
            showSnackbar(getString(R.string.failover_trying, mirrorIndex + 1, next + 1))
            loadMirror(next, userChoice = false)
            return
        }
        mainHandler.removeCallbacks(landingTimeout)
        val allTried = landingLoad && mirrors.size > 1
        if (allTried) _binding?.webview?.stopLoading()
        showErrorDialog(
            if (allTried) getString(R.string.error_all_busy, mirrors.size) else message,
            offerWatch,
            reloadLanding = landingLoad
        )
    }

    /** [offerWatch] adds "notify me when it's back" for server-side failures. */
    private fun showErrorDialog(message: String, offerWatch: Boolean = false, reloadLanding: Boolean = false) {
        loadFailed = true
        _binding?.savePdfBtn?.visibility = View.GONE
        if (!isAdded || isDetached) return
        dialog?.dismiss()
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.error_retry) { _, _ ->
                // Before a page loaded, retry this mirror from the start (and fail over again);
                // after, reload so a submitted form is re-sent
                if (reloadLanding) loadMirror(mirrorIndex, userChoice = true) else binding.webview.reload()
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
        if (mirrors.isEmpty()) return
        val type = arguments?.getString("type") ?: return
        val label = dashboardItemList.firstOrNull { it.type.name == type }
            ?.let { getString(it.title) } ?: type
        ServerWatch.start(requireContext(), type, label, mirrors)
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

    companion object {
        // A mirror whose first page doesn't finish loading in this time counts as busy
        private const val LANDING_TIMEOUT_MS = 30_000L
    }
}
