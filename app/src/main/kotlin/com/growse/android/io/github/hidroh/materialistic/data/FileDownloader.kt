package com.growse.android.io.github.hidroh.materialistic.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink

@Singleton
class FileDownloader
@Inject
constructor(
    @ApplicationContext context: Context,
    private val mCallFactory: Call.Factory,
) {
  private val mCacheDir: String =
      context.cacheDir.path // don't need to keep a reference to context after this
  val mMainHandler: Handler = Handler(Looper.getMainLooper())

  @WorkerThread
  fun downloadFile(url: String, mimeType: String, callback: FileDownloaderCallback) {
    val outputFile = File(mCacheDir, File(url).name)
    if (outputFile.exists()) {
      mMainHandler.post { callback.onSuccess(outputFile.path) }
      return
    }

    val request = Request.Builder().url(url).addHeader("Content-Type", mimeType).build()

    mCallFactory
        .newCall(request)
        .enqueue(
            object : Callback {
              override fun onFailure(call: Call, e: okio.IOException) {
                mMainHandler.post { callback.onFailure(call, e) }
              }

              override fun onResponse(call: Call, response: Response) {
                try {
                  val sink = outputFile.sink().buffer()
                  sink.writeAll(response.body.source())
                  sink.close()
                  mMainHandler.post { callback.onSuccess(outputFile.path) }
                } catch (e: IOException) {
                  this.onFailure(call, e)
                }
              }
            }
        )
  }

  interface FileDownloaderCallback {
    fun onFailure(call: Call?, e: IOException?)

    fun onSuccess(filePath: String?)
  }
}
