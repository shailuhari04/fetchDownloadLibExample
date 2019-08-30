package com.example.android.downloadmanager.global

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import java.util.ArrayList

object FetchConfigUtils {

    private const val TAG = "FetchConfigUtils"
    const val FETCH_NAMESPACE = "FetchConfigUtilsNameSpace"

    private fun getFetchConfig(context: Context): FetchConfiguration {
        return FetchConfiguration.Builder(context)
            .enableLogging(true)
            .enableRetryOnNetworkGain(true)
            //.setHttpDownloader(OkHttpDownloader(OkHttpClient.Builder().build()))
            .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
            .setNamespace(FETCH_NAMESPACE)
            .setNotificationManager(object : DefaultFetchNotificationManager(context) {
                override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                    return fetch!!
                }
            })
            .setDownloadConcurrentLimit(4)//maxValue
            .build()

    }

    private val fetch: Fetch? = null

    /**
     * @return Singleton Instance of Fetch
     */
    fun fetchInstance(context: Context): Fetch {
        return fetch ?: Fetch.Impl.getInstance(getFetchConfig(context))
    }

    var mRequestDataList = mutableListOf<RequestData>()

    fun addRequest(requestData: RequestData) {
        mRequestDataList.add(requestData)
    }

    @NonNull
    private fun getFetchRequests(): List<Request> {
        val requests = ArrayList<Request>()
        for (requestData in mRequestDataList) {
            val request = Request(requestData.url, requestData.filePath)
            requests.add(request)
        }
        return requests
    }

    @NonNull
    fun getFetchRequestWithGroupId(groupId: Int): List<Request> {
        val requests = getFetchRequests()
        for (request in requests) {
            request.groupId = groupId
        }
        return requests
    }

    @NonNull
    private fun getFilePath(@NonNull url: String): String {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment
        val dir = getSaveDir()
        return "$dir/$fileName"
    }

    @NonNull
    fun getSaveDir(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .toString()
    }


    class RequestData {
        var id: Int = 0
        @Nullable
        var request: Request? = null
        internal var fileName: String = ""
        internal var url: String = ""
        internal var filePath: String = ""
        internal var fileUri: Uri? = null
        internal var networkType: NetworkType = NetworkType.ALL
        internal var priority: Priority = Priority.NORMAL

        override fun hashCode(): Int {
            return id
        }

        override fun toString(): String {
            return if (request == null) {
                ""
            } else request!!.toString()
        }

        override fun equals(other: Any?): Boolean {
            return other === this || other is RequestData && other.id == id
        }
    }
}

