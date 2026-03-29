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

package io.github.hidroh.materialistic.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.CallSuper
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient

import java.io.File
import java.util.Map

import io.github.hidroh.materialistic.AppUtils

open class CacheableWebView : WebView() {
    private const val CACHE_PREFIX: String = "webarchive-"
    private const val CACHE_EXTENSION: String = ".mht"
    private var mArchiveClient: ArchiveClient = new ArchiveClient()

    constructor(context: Context) {
        this(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        super(context, attrs, defStyleAttr)
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
        super.loadUrl(getCacheableUrl(url))
    }

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        mArchiveClient.lastProgress = 0
        super.loadUrl(getCacheableUrl(url), additionalHttpHeaders)
    }

    override fun setWebChromeClient(client: WebChromeClient) {
        if (!(client is ArchiveClient)) {
            throw IllegalArgumentException("client should be an instance of " +
                    ArchiveClient::class.java.getName())
        }
        mArchiveClient = (ArchiveClient) client
        super.setWebChromeClient(mArchiveClient)
    }

    private fun init() {
        enableCache()
        setLoadSettings()
        setWebViewClient(WebViewClient())
        setWebChromeClient(mArchiveClient)
    }

    private fun enableCache() {
        WebSettings webSettings = getSettings()
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK)
        webSettings.setAllowFileAccess(true)
        setCacheModeInternal()
    }

    private fun setCacheModeInternal() {
        getSettings().setCacheMode(AppUtils.hasConnection(getContext()) ?
                WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_CACHE_ONLY)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setLoadSettings() {
        WebSettings webSettings = getSettings()
        webSettings.setLoadWithOverviewMode(true)
        webSettings.setUseWideViewPort(true)
        webSettings.setJavaScriptEnabled(true)
    }

    private fun getCacheableUrl(url: String): String {
        if (TextUtils.equals(url, BLANK) || TextUtils.equals(url, FILE)) {
            return url
        }
        mArchiveClient.cacheFileName = generateCacheFilename(url)
        setCacheModeInternal()
        if (getSettings().getCacheMode() != WebSettings.LOAD_CACHE_ONLY) {
            return url
        }
        File cacheFile = File(mArchiveClient.cacheFileName)
        return cacheFile.exists() ? Uri.fromFile(cacheFile).toString() : url
    }

    private fun generateCacheFilename(url: String): String {
        return getContext().getApplicationContext().getCacheDir().getAbsolutePath() +
                File.separator +
                CACHE_PREFIX +
                url.hashCode() +
                CACHE_EXTENSION
    }

    open class ArchiveClient : WebChromeClient() {
        var lastProgress: Int = 0
        var cacheFileName: String? = null

        @CallSuper
        override fun onProgressChanged(view: android.webkit.WebView, newProgress: Int) {
            if (view.getSettings().getCacheMode() == WebSettings.LOAD_CACHE_ONLY) {
                return
            }
            if (cacheFileName != null && lastProgress != 100 && newProgress == 100) {
                lastProgress = newProgress
                view.saveWebArchive(cacheFileName)
            }
        }

    }
}
