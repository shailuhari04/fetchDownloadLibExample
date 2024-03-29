package com.example.android.downloadmanager.utility

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.android.downloadmanager.R
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.util.*

object DownloadUtils {

    fun getMimeType(context: Context, uri: Uri): String {
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = mime.getExtensionFromMimeType(cR.getType(uri))
        if (type == null) {
            type = "*/*"
        }
        return type
    }

    fun deleteFileAndContents(file: File) {
        if (file.exists()) {
            if (file.isDirectory) {
                val contents = file.listFiles()
                if (contents != null) {
                    for (content in contents) {
                        deleteFileAndContents(
                            content
                        )
                    }
                }
            }
            file.delete()
        }
    }

    fun getETAString(context: Context, etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds < 0) {
            return ""
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return if (hours > 0) {
            context.getString(R.string.download_eta_hrs, hours, minutes, seconds)
        } else if (minutes > 0) {
            context.getString(R.string.download_eta_min, minutes, seconds)
        } else {
            context.getString(R.string.download_eta_sec, seconds)
        }
    }

    fun getDownloadSpeedString(context: Context, downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return ""
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val decimalFormat = DecimalFormat(".##")
        return if (mb >= 1) {
            context.getString(R.string.download_speed_mb, decimalFormat.format(mb))
        } else if (kb >= 1) {
            context.getString(R.string.download_speed_kb, decimalFormat.format(kb))
        } else {
            context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond)
        }
    }

    fun createFile(filePath: String): File {
        val file = File(filePath)
        if (!file.exists()) {
            val parent = file.parentFile
            if (!parent!!.exists()) {
                parent.mkdirs()
            }
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return file
    }

    fun getProgress(downloaded: Long, total: Long): Int {
        return if (total < 1) {
            -1
        } else if (downloaded < 1) {
            0
        } else if (downloaded >= total) {
            100
        } else {
            (downloaded.toDouble() / total.toDouble() * 100).toInt()
        }
    }

    fun getBytesToMBString(context: Context, bytes: Long): String {
        if (bytes < 0) {
            return ""
        }
        val kb = bytes.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val decimalFormat = DecimalFormat(".##")
        return if (mb >= 1) {
            context.getString(R.string.download_size_mb, decimalFormat.format(mb))
        } else if (kb >= 1) {
            context.getString(R.string.download_size_kb, decimalFormat.format(kb))
        } else {
            context.getString(R.string.download_size_bytes, bytes)
        }
    }

}
