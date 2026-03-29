package io.github.hidroh.materialistic.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread

import java.io.File
import java.io.IOException

import javax.inject.Inject

import io.github.hidroh.materialistic.annotation.Synthetic
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.Okio

open class FileDownloader {
    private var mCallFactory: Call.Factory? = null
    private var mCacheDir: String? = null
    var mMainHandler: Handler? = null

    @Inject
    constructor(context: Context, callFactory: Call.Factory) {
        mCacheDir = context.getCacheDir().getPath(); // don't need to keep a reference to context after this
        mCallFactory = callFactory
        mMainHandler = Handler(Looper.getMainLooper())
    }

    @WorkerThread
    fun downloadFile(url: String, mimeType: String, callback: FileDownloaderCallback) {
        File outputFile = File(mCacheDir, File(url).getName())
        if (outputFile.exists()) {
            mMainHandler.post(() -> callback.onSuccess(outputFile.getPath()))
            return
        }

        final Request request = Request.Builder().url(url)
                .addHeader("Content-Type", mimeType)
                .build()

        mCallFactory.newCall(request).enqueue(Callback() {
            public void onFailure(Call call, IOException e) {
                mMainHandler.post(() -> callback.onFailure(call, e))
            }

            public void onResponse(Call call, Response response) {
                try {
                    BufferedSink sink = Okio.buffer(Okio.sink(outputFile))
                    sink.writeAll(response.body().source())
                    sink.close()
                    mMainHandler.post(() -> callback.onSuccess(outputFile.getPath()))
                } catch (IOException e) {
                    this.onFailure(call, e)
                }
            }
        })
    }

    interface FileDownloaderCallback {
        fun onFailure(call: Call, e: IOException)
        fun onSuccess(filePath: String)
    }
}
