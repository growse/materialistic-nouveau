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
package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.growse.android.io.github.hidroh.materialistic.AppUtils.urlEquals

open class WebView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr) {
  private val mClient = HistoryWebViewClient()
  var mPendingUrl: String? = null
  var mPendingHtml: String? = null

  init {
    super.setWebViewClient(mClient)
  }

  override fun setWebViewClient(client: WebViewClient) {
    mClient.wrap(client)
  }

  override fun canGoBack(): Boolean {
    return TextUtils.isEmpty(mPendingUrl) && super.canGoBack()
  }

  open fun reloadUrl(url: String) {
    if (progress < 100) {
      stopLoading() // this will fire onPageFinished for current URL
    }
    mPendingUrl = url
    loadUrl(BLANK) // clear current web resources, load pending URL upon onPageFinished
  }

  fun reloadHtml(html: String?) {
    mPendingHtml = html
    reloadUrl(FILE)
  }

  internal class HistoryWebViewClient : WebViewClient() {
    private var mClient: WebViewClient? = null

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
      super.onPageStarted(view, url, favicon)
      view.pageUp(true)
      val webView = view as com.growse.android.io.github.hidroh.materialistic.WebView
      if (urlEquals(url, webView.mPendingUrl)) {
        view.visibility = VISIBLE
      }
      if (mClient != null) {
        mClient!!.onPageStarted(view, url, favicon)
      }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
      super.onPageFinished(view, url)
      val webView = view as com.growse.android.io.github.hidroh.materialistic.WebView
      if (TextUtils.equals(url, BLANK)) { // has pending reload, open corresponding URL
        if (!TextUtils.isEmpty(webView.mPendingHtml)) {
          view.loadDataWithBaseURL(
              webView.mPendingUrl,
              webView.mPendingHtml!!,
              "text/html",
              "UTF-8",
              webView.mPendingUrl,
          )
        } else {
          view.loadUrl(webView.mPendingUrl!!)
        }
      } else if (
          !TextUtils.isEmpty(webView.mPendingUrl) && TextUtils.equals(url, webView.mPendingUrl)
      ) { // reload done, clear history
        webView.mPendingUrl = null
        webView.mPendingHtml = null
        view.clearHistory()
      }
      if (mClient != null) {
        mClient!!.onPageFinished(view, url)
      }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
      return if (mClient != null)
          mClient!!.shouldInterceptRequest(
              view,
              url,
          )
      else super.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
      return if (mClient != null)
          mClient!!.shouldInterceptRequest(
              view,
              request,
          )
      else super.shouldInterceptRequest(view, request)
    }

    fun wrap(client: WebViewClient?) {
      mClient = client
    }
  }

  companion object {
    const val BLANK: String = "about:blank"
    const val FILE: String = "file:///"
  }
}
