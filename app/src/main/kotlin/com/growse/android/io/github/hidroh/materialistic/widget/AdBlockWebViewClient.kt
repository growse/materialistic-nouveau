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

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.growse.android.io.github.hidroh.materialistic.AdBlocker

open class AdBlockWebViewClient(private val adBlockEnabled: Boolean) : WebViewClient() {
  private val loadedUrls = HashMap<String, Boolean>()

  override fun shouldInterceptRequest(
      view: WebView?,
      request: WebResourceRequest,
  ): WebResourceResponse? {
    if (!adBlockEnabled) {
      return super.shouldInterceptRequest(view, request)
    }
    val url = request.url.toString()
    val ad = loadedUrls.getOrPut(url) { AdBlocker.isAd(url) }
    return if (ad) AdBlocker.createEmptyResource() else super.shouldInterceptRequest(view, request)
  }
}
