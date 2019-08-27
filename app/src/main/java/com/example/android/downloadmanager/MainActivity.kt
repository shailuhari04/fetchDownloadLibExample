package com.example.android.downloadmanager

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2core.Reason
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.download_item.*
import okhttp3.OkHttpClient
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity(), FetchListener, FetchObserver<Download> {

    //Setting download Id for the per download
    private lateinit var fetch: Fetch
    private val TAG = "MainActivity"
    private var request: Request? = null
    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetchSetUp()

        webViewSetUp()

        permissionSetUp()

        /* To download the file we have to implement this method , Used third party library*/
        downloadFieListenerSetUp()

        setEventListener();

    }

    private fun setEventListener() {
        iv_pause_resume?.setOnClickListener {
            iv_pause_resume.setImageResource(R.drawable.ic_play)
            fetch.pause(request?.id!!)
        }
    }


    override fun onResume() {
        super.onResume()
        if (request != null) {
            fetch.attachFetchObserversForDownload(request?.id!!, this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (request != null) {
            fetch.removeFetchObserversForDownload(request?.id!!, this)
        }
    }

    private fun permissionSetUp() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    private fun fetchSetUp() {
        val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(this)
            .enableLogging(true)
            .enableRetryOnNetworkGain(true)
            .setHttpDownloader(OkHttpDownloader(OkHttpClient.Builder().build()))
            .setDownloadConcurrentLimit(4)//maxValue
            .build()
        fetch = Fetch.Impl.getInstance(fetchConfiguration)

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun webViewSetUp() {
        webview_dwnld.loadUrl("https://www.google.com")
        webview_dwnld.settings.javaScriptEnabled = true

        /* Setting Up the webView Client*/
        webview_dwnld.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                group.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {

                /*To maintain the session of the user We use flush() method this maintain the session unless logged out
                * See official documentation fo rmore details
                 * */
                val cookieManager: CookieManager = CookieManager.getInstance()
                cookieManager.flush()
                group.visibility = View.GONE
                super.onPageFinished(view, url)

            }
        }

        /* Setting Up the web chrome client*/
        webview_dwnld.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                pb_loading.progress = newProgress
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                iv_favicon.setImageBitmap(icon)

            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                supportActionBar?.title = title
            }
        }
    }

    private fun downloadFieListenerSetUp() {
        webview_dwnld.setDownloadListener { url, userAgent, contentDescription, mimetype, contentLength ->
            /*Downloaded File Path*/
            // val dirPath: String = getExternalFilesDir(null)!!.absolutePath
            val dirPath: String =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
            /*Parsing the name from the url*/
            fileName = URLUtil.guessFileName(url, contentDescription, mimetype)

            tv_dwnldng_file_name?.setText(fileName)
            tv_spd_paus?.setText(R.string.downloading)
            val filePath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName

            Log.e(
                TAG,
                "\n dirPath  $dirPath \n fileName $fileName  \n filePath $filePath \n filePath ${filePath}"
            )

            //setRequest
            request = Request(url, filePath)
            request?.priority = Priority.HIGH
            request?.networkType = NetworkType.ALL

            fetch.enqueue(request!!, object : Func<Request> {
                override fun call(result: Request) {
                    Log.e(TAG, "enqueue call  ${result.url}")
                }
            }, object : Func<Error> {
                override fun call(error: Error) {
                    Log.e(TAG, "enqueue error call  ${error.value}")
                }
            })

            fetch.addListener(this)//set Listener
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch.close()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater: MenuInflater = menuInflater
        menuInflater.inflate(R.menu.reload_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                webview_dwnld.reload()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (webview_dwnld.canGoBack()) {
            webview_dwnld.goBack()
        } else {
            finish()
        }
    }

    override fun onChanged(download: Download, reason: Reason) {
        updateViews(download, reason)
    }


    private fun updateViews(download: Download, reason: Reason) {
        if (request?.id == download.id) {
            if (reason == Reason.DOWNLOAD_QUEUED || reason == Reason.DOWNLOAD_COMPLETED) {
                val uri = Uri.parse(download.file)
                tv_dwnldng_file_name.setText(uri.lastPathSegment)
            }
            setProgressView(download.status, download)
            // etaTextView.setText(Utils.getETAString(this, download.etaInMilliSeconds))
            if (download.error != Error.NONE) {
                Log.e(TAG, "error ${download.error}")
            }
        }
    }


    private fun setProgressView(@NonNull status: Status, download: Download) {
        when (status) {
            Status.QUEUED -> {
                tv_spd_paus.setText(R.string.queued)
            }
            Status.ADDED -> {
                tv_spd_paus.setText(R.string.added)
            }
            Status.COMPLETED -> {
                tv_spd_paus.setText(R.string.completed)
            }
            Status.DOWNLOADING -> {
                tv_spd_paus.setText(
                    Utils.getDownloadSpeedString(
                        this,
                        download.downloadedBytesPerSecond
                    )
                )
            }
            else -> {
                tv_spd_paus.setText("")
            }
        }
    }


    override fun onAdded(download: Download) {
        Log.e(TAG, "onAdded  ${download.status}")
        if (request?.id == download.id) {
            tv_file_size_total.text = (download.total / 8000).toString()
            tv_dwnldng_file_name.text = fileName
        }
    }

    override fun onCancelled(download: Download) {
        Log.e(TAG, "Download Cancelled  ${download.status}")
    }

    override fun onCompleted(download: Download) {
        Log.e(TAG, "onCompleted  ${download.status}")
        try {
            tv_spd_paus?.setText(R.string.completed)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDeleted(download: Download) {
        Log.e(TAG, "onDeleted  ${download.status}")
    }

    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
        Log.e(TAG, "onDownloadBlockUpdated  ${download.status}")
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        Log.e(TAG, "onDownloadBlockUpdated  $error ---- ${download.status}")
    }

    override fun onPaused(download: Download) {
        iv_pause_resume.setImageResource(R.drawable.ic_play)
        iv_pause_resume.setOnClickListener {
            fetch.resume(request?.id!!)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        if (request?.id == download.id) {

            pb_dwnlding.progress = download.progress
            tv_file_size_total.text =
                getBytesToMBString(download.downloaded) + "/" + getBytesToMBString(download.total)
            tv_spd_paus.text = getBytesToMBString(downloadedBytesPerSecond)

            //(downloadedBytesPerSecond / 8000)
        }

    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        if (request?.id == download.id) {
            Log.e(TAG, "onQueued  ${download.status}")
        }

    }

    override fun onRemoved(download: Download) {
        Log.e(TAG, "onRemoved  ${download.status}")
    }

    override fun onResumed(download: Download) {
        Log.e(TAG, "onResumed  ${download.status}")
        if (request?.id == download.id) {
            iv_pause_resume.setImageResource(R.drawable.ic_pause)
            iv_pause_resume.setOnClickListener {
                fetch.pause(request?.id!!)
            }
        }
    }


    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {

        Log.e(TAG, "onStarted  ${download.status}")
        if (request?.id == download.id) {

            Log.e(TAG, "onStarted  ${download.status}")
        }

    }

    override fun onWaitingNetwork(download: Download) {
    }

    private fun getBytesToMBString(bytes: Long): String {
        return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00))
    }
}

