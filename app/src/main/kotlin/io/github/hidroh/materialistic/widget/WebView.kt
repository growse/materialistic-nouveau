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

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.annotation.Synthetic

open class WebView : android.webkit.WebView() {
    const val BLANK: String = "about:blank"
    const val FILE: String = "file:///"
    private val mClient: HistoryWebViewClient = new HistoryWebViewClient()
    var mPendingHtml: String mPendingUrl,? = null

    constructor(context: Context) {
        this(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) {
        this(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        super.setWebViewClient(mClient)
    }

    override fun setWebViewClient(client: WebViewClient) {
        mClient.wrap(client)
    }

    override fun canGoBack(): Boolean {
        return TextUtils.isEmpty(mPendingUrl) && super.canGoBack()
    }

    fun reloadUrl(url: String) {
        if (getProgress() < 100) {
            stopLoading(); // this will fire onPageFinished for current URL
        }
        mPendingUrl = url
        loadUrl(BLANK); // clear current web resources, load pending URL upon onPageFinished
    }

    fun reloadHtml(html: String) {
        mPendingHtml = html
        reloadUrl(FILE)
    }

    open class HistoryWebViewClient : WebViewClient() {
        private var mClient: WebViewClient? = null

        override fun onPageStarted(view: android.webkit.WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
            view.pageUp(true)
            val webView = (WebView) view
            if (AppUtils.urlEquals(url, webView.mPendingUrl)) {
                view.setVisibility(VISIBLE)
            }
            if (mClient != null) {
                mClient.onPageStarted(view, url, favicon)
            }
        }

        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            super.onPageFinished(view, url)
            val webView = (WebView) view
            if (TextUtils.equals(url, BLANK)) { // has pending reload, open corresponding URL
                if (!TextUtils.isEmpty(webView.mPendingHtml)) {
                    view.loadDataWithBaseURL(webView.mPendingUrl, webView.mPendingHtml,
                            "text/html", "UTF-8", webView.mPendingUrl)
                } else {
                    view.loadUrl(webView.mPendingUrl)
                }
            } else if (!TextUtils.isEmpty(webView.mPendingUrl) &&
                    TextUtils.equals(url, webView.mPendingUrl)) { // reload done, clear history
                webView.mPendingUrl = null
                webView.mPendingHtml = null
                view.clearHistory()
            }
            if (mClient != null) {
                mClient.onPageFinished(view, url)
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @SuppressWarnings("deprecation")
        override fun shouldInterceptRequest(view: android.webkit.WebView, url: String): WebResourceResponse {
            return mClient != null ? mClient.shouldInterceptRequest(view, url) :
                    super.shouldInterceptRequest(view, url)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(view: android.webkit.WebView, request: WebResourceRequest): WebResourceResponse {
            return mClient != null ? mClient.shouldInterceptRequest(view, request) :
                    super.shouldInterceptRequest(view, request)
        }

        @Synthetic
        fun wrap(client: WebViewClient) {
            mClient = client
        }
    }
}
