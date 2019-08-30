package com.example.android.downloadmanager.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.downloadmanager.R
import com.example.android.downloadmanager.adapter.DownloadFileListAdapter
import com.example.android.downloadmanager.callback_listener.AdapterItemClickListener
import com.example.android.downloadmanager.global.FetchConfigUtils
import com.example.android.downloadmanager.utility.DownloadUtils
import com.google.android.material.snackbar.Snackbar
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2core.Reason
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.download_item.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), FetchListener, FetchObserver<Download>,
    AdapterItemClickListener {

    private lateinit var mainView: View
    private lateinit var fetch: Fetch
    private val TAG = "MainActivity"
    private lateinit var fileName: String
    private val GROUP_ID = "listGroup".hashCode()
    private val UNKNOWN_REMAINING_TIME: Long = -1
    private val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
    private val STORAGE_PERMISSION_CODE = 200


    private var fileAdapter: DownloadFileListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainView = findViewById(R.id.webview_dwnld)

        fetchSetUp()

        webViewSetUp()

        recyclerViewSetUp()

        webViewListenerSetUp()
    }

    private fun recyclerViewSetUp() {
        fileAdapter = DownloadFileListAdapter(this)
        rv_download_list.layoutManager = LinearLayoutManager(this)
        rv_download_list.adapter = fileAdapter
    }


    override fun onResume() {
        super.onResume()

        fetch.getDownloadsInGroup(GROUP_ID, Func { downloadList ->
            val list = ArrayList<Download>(downloadList)
            list.sortWith(kotlin.Comparator { first, second ->
                first.created.compareTo(second.created)
            })

            for (download in list) {
                fileAdapter?.addDownload(download)
            }

        }).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        fetch.removeListener(this)
    }

    private fun fetchSetUp() {
        fetch = FetchConfigUtils.fetchInstance(this)
        checkStoragePermissions()
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

    private fun webViewListenerSetUp() {
        webview_dwnld.setDownloadListener { url, userAgent, contentDescription, mimetype, contentLength ->
            /*Downloaded File Path*/
            // val dirPath: String = getExternalFilesDir(null)!!.absolutePath
            val dirPath: String =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
            /*Parsing the name from the url*/
            fileName = URLUtil.guessFileName(url, contentDescription, mimetype)

            val filePath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName

            Log.e(
                TAG,
                "\n contentDescription $contentDescription \n url $url \n dirPath  $dirPath \n fileName $fileName  \n filePath $filePath \n filePath ${filePath}"
            )

            val requestData: FetchConfigUtils.RequestData = FetchConfigUtils.RequestData()
            requestData.url = url
            requestData.filePath = filePath
            FetchConfigUtils.addRequest(requestData)
            checkStoragePermissions()
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

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            enqueueDownloads()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownloads()
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE)
                .show()
        }
    }

    private fun enqueueDownloads() {
        val requests = FetchConfigUtils.getFetchRequestWithGroupId(GROUP_ID)
        fetch.enqueue(requests, Func { updatedRequests ->
            Log.e(TAG, "enqueue ${updatedRequests.size}")
        })
    }

    override fun onChanged(data: Download, reason: Reason) {
        Log.e(TAG, "onChanged ${data.status} reason ${reason.value}")
        //  updateViews(data, reason)
    }



    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
        Log.e(TAG, "onDownloadBlockUpdated  ${download.status}")
    }

    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {
        Log.e(TAG, "onStarted  ${download.status}")
    }

    override fun onAdded(download: Download) {
        Log.e(TAG, "onAdded  ${download.status}")
        fileAdapter?.addDownload(download)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        Log.e(TAG, "onQueued  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onCompleted(download: Download) {
        Log.e(TAG, "onCompleted  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        // super.onError(download = download, error = error, throwable = throwable)
        Log.e(TAG, "onDownloadBlockUpdated  $error ---- ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        fileAdapter?.update(download, etaInMilliSeconds, downloadedBytesPerSecond)
    }

    override fun onPaused(download: Download) {
        Log.e(TAG, "onPaused  $download ---- ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onResumed(download: Download) {
        Log.e(TAG, "onResumed  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onCancelled(download: Download) {
        Log.e(TAG, "Download onCancelled  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onRemoved(download: Download) {
        Log.e(TAG, "Download onRemoved  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onDeleted(download: Download) {
        Log.e(TAG, "onDeleted  ${download.status}")
        fileAdapter?.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
    }

    override fun onWaitingNetwork(download: Download) {
        Log.e(TAG, "onWaitingNetwork  ${download.status}")
    }

    override fun onPauseDownload(id: Int) {
        fetch.pause(id)
    }

    override fun onResumeDownload(id: Int) {
        fetch.resume(id)
    }

    override fun onRemoveDownload(id: Int) {
        fetch.remove(id)
    }

    override fun onRetryDownload(id: Int) {
        fetch.retry(id)
    }
}

