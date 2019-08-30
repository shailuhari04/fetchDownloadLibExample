package com.example.android.downloadmanager.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.android.downloadmanager.R
import com.example.android.downloadmanager.callback_listener.AdapterItemClickListener
import com.example.android.downloadmanager.databinding.DownloadItemBinding
import com.example.android.downloadmanager.utility.DownloadUtils
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import java.io.File
import java.util.*

class DownloadFileListAdapter(private val actionListener: AdapterItemClickListener) :
    RecyclerView.Adapter<DownloadFileListAdapter.DownloadFileViewHolder>() {

    private val downloads = ArrayList<DownloadData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadFileViewHolder {
        val binding: DownloadItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.download_item,
            parent,
            false
        )
        return DownloadFileViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(@NonNull holder: DownloadFileViewHolder, position: Int) {
        holder.binding.ivAction.setOnClickListener(null)
        holder.binding.ivAction.isEnabled = true

        val downloadData = downloads[position]
        var url = ""
        if (downloadData.download != null) {
            url = downloadData.download!!.url
        }
        val uri = Uri.parse(url)
        val status = downloadData.download?.status
        val context = holder.itemView.context

        holder.binding.tvDwnldngFileName.text = "${(downloadData.download?.file?.lastIndexOf('/'))?.plus(
            1
        )?.let { downloadData.download?.file?.substring(it) }}"

        holder.binding.tvSpdPaus.text = DownloadUtils.getDownloadSpeedString(
            context,
            downloadData.download!!.downloadedBytesPerSecond
        ) + (if (getStatusString(status!!) != "Downloading") getStatusString(status) else "")

        holder.binding.tvFileSizeTotal.text = DownloadUtils.getBytesToMBString(
            context,
            downloadData.download!!.downloaded
        ) + "/" + DownloadUtils.getBytesToMBString(context, downloadData.download!!.total)

        var progress = downloadData.download!!.progress
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0
        }
        holder.binding.pbDwnlding.progress = progress

        when (status) {
            Status.COMPLETED -> {
                holder.binding.ivAction.setImageResource(R.drawable.ic_check_circle_black_24dp)
                holder.binding.ivAction.setOnClickListener { view ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Toast.makeText(
                            context,
                            "Downloaded Path:" + downloadData.download!!.file,
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    val file = File(downloadData.download!!.file)
                    val uri1 = Uri.fromFile(file)
                    val share = Intent(Intent.ACTION_VIEW)
                    share.setDataAndType(uri1, DownloadUtils.getMimeType(context, uri1))
                    context.startActivity(share)
                }
            }
            Status.FAILED -> {
                holder.binding.ivAction.setImageResource(R.drawable.ic_refresh_black_24dp)
                holder.binding.ivAction.setOnClickListener { view ->
                    holder.binding.ivAction.isEnabled = false
                    actionListener.onRetryDownload(downloadData.download!!.id)
                }
            }
            Status.PAUSED -> {
                holder.binding.ivAction.setImageResource(R.drawable.ic_play)
                holder.binding.ivAction.setOnClickListener { view ->
                    holder.binding.ivAction.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            Status.DOWNLOADING, Status.QUEUED -> {
                holder.binding.ivAction.setImageResource(R.drawable.ic_pause)
                holder.binding.ivAction.setOnClickListener { view ->
                    holder.binding.ivAction.isEnabled = false
                    actionListener.onPauseDownload(downloadData.download!!.id)
                }
            }
            Status.ADDED -> {
                holder.binding.ivAction.setImageResource(R.drawable.ic_pause)
                holder.binding.ivAction.setOnClickListener { view ->
                    holder.binding.ivAction.isEnabled = false
                    actionListener.onResumeDownload(downloadData.download!!.id)
                }
            }
            else -> {
            }
        }
        //Set delete action
        holder.itemView.setOnLongClickListener { v ->
            val uri12 = Uri.parse(downloadData.download?.url)
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_title))
                .setMessage(uri12.lastPathSegment)
                .setPositiveButton(
                    R.string.delete
                ) { dialog, which ->
                    downloadData.download?.id?.let {
                        actionListener.onRemoveDownload(
                            it
                        )
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

            true
        }

    }

    fun addDownload(@NonNull download: Download) {
        var found = false
        var data: DownloadData? = null
        var dataPosition = -1
        for (i in downloads.indices) {
            val downloadData = downloads[i]
            if (downloadData.id == download.id) {
                data = downloadData
                dataPosition = i
                found = true
                break
            }
        }
        if (!found) {
            val downloadData = DownloadData()
            downloadData.id = download.id
            downloadData.download = download
            downloads.add(downloadData)
            notifyItemInserted(downloads.size - 1)
        } else {
            data!!.download = download
            notifyItemChanged(dataPosition)
        }
    }

    override fun getItemCount(): Int {
        return downloads.size
    }

    fun update(@NonNull download: Download, eta: Long, downloadedBytesPerSecond: Long) {
        for (position in downloads.indices) {
            val downloadData = downloads[position]
            if (downloadData.id == download.id) {
                when (download.status) {
                    Status.REMOVED, Status.DELETED -> {
                        downloads.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    else -> {
                        downloadData.download = download
                        downloadData.eta = eta
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond
                        notifyItemChanged(position)
                    }
                }
                return
            }
        }
    }

    private fun getStatusString(status: Status): String {
        return when (status) {
            Status.COMPLETED -> "Done"
            Status.DOWNLOADING -> "Downloading"
            Status.FAILED -> "Error"
            Status.PAUSED -> "Paused"
            Status.QUEUED -> "Waiting in Queue"
            Status.REMOVED -> "Removed"
            Status.NONE -> "Not Queued"
            else -> "Unknown"
        }
    }


    class DownloadFileViewHolder(val binding: DownloadItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class DownloadData {
    var id: Int = 0
    @Nullable
    var download: Download? = null
    internal var eta: Long = -1
    internal var downloadedBytesPerSecond: Long = 0

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return if (download == null) {
            ""
        } else download!!.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other === this || other is DownloadData && other.id == id
    }
}
