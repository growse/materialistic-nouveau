/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growse.android.io.github.hidroh.materialistic.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.CallSuper
import com.growse.android.io.github.hidroh.materialistic.AppUtils.hasConnection
import java.io.File

class CacheableWebView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : com.growse.android.io.github.hidroh.materialistic.WebView(context, attrs, defStyleAttr) {
  private var mArchiveClient = ArchiveClient()

  init {
    init()
  }

  override fun reloadUrl(url: String) {
    super.reloadUrl(getCacheableUrl(url))
  }

  override fun loadUrl(url: String) {
    if (TextUtils.isEmpty(url)) {
      return
    }
    mArchiveClient.lastProgress = 0
    super.loadUrl(getCacheableUrl(url)!!)
  }

  override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String?, String?>) {
    if (TextUtils.isEmpty(url)) {
      return
    }
    mArchiveClient.lastProgress = 0
    super.loadUrl(getCacheableUrl(url)!!, additionalHttpHeaders)
  }

  override fun setWebChromeClient(client: WebChromeClient?) {
    require(client is ArchiveClient) {
      "client should be an instance of " + ArchiveClient::class.java.name
    }
    mArchiveClient = client
    super.setWebChromeClient(mArchiveClient)
  }

  private fun init() {
    enableCache()
    setLoadSettings()
    webViewClient = WebViewClient()
    setWebChromeClient(mArchiveClient)
  }

  private fun enableCache() {
    val webSettings = settings
    webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    webSettings.allowFileAccess = true
    setCacheModeInternal()
  }

  private fun setCacheModeInternal() {
    settings.cacheMode =
        if (hasConnection(context)) WebSettings.LOAD_CACHE_ELSE_NETWORK
        else WebSettings.LOAD_CACHE_ONLY
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun setLoadSettings() {
    val webSettings = settings
    webSettings.loadWithOverviewMode = true
    webSettings.useWideViewPort = true
    webSettings.javaScriptEnabled = true
  }

  private fun getCacheableUrl(url: String): String {
    if (TextUtils.equals(url, BLANK) || TextUtils.equals(url, FILE)) {
      return url
    }
    mArchiveClient.cacheFileName = generateCacheFilename(url)
    setCacheModeInternal()
    if (settings.cacheMode != WebSettings.LOAD_CACHE_ONLY) {
      return url
    }
    val cacheFile = File(mArchiveClient.cacheFileName)
    return if (cacheFile.exists()) Uri.fromFile(cacheFile).toString() else url
  }

  private fun generateCacheFilename(url: String): String {
    return context.applicationContext.cacheDir.absolutePath +
        File.separator +
        CACHE_PREFIX +
        url.hashCode() +
        CACHE_EXTENSION
  }

  open class ArchiveClient : WebChromeClient() {
    var lastProgress: Int = 0
    var cacheFileName: String? = null

    @CallSuper
    override fun onProgressChanged(view: WebView, newProgress: Int) {
      if (view.settings.cacheMode == WebSettings.LOAD_CACHE_ONLY) {
        return
      }
      if (cacheFileName != null && lastProgress != 100 && newProgress == 100) {
        lastProgress = newProgress
        view.saveWebArchive(cacheFileName!!)
      }
    }
  }

  companion object {
    private const val CACHE_PREFIX = "webarchive-"
    private const val CACHE_EXTENSION = ".mht"
  }
}
