package com.example.android.downloadmanager.callback_listener

interface AdapterItemClickListener {
  //  fun onItemClick(obj: Any, position: Int, action: String)

     fun onPauseDownload(id: Int)

     fun onResumeDownload(id: Int)

     fun onRemoveDownload(id: Int)

     fun onRetryDownload(id: Int)
}