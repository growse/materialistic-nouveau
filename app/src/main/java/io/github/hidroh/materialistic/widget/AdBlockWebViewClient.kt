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
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import java.util.HashMap
import java.util.Map

import androidx.annotation.Nullable
import io.github.hidroh.materialistic.AdBlocker

open class AdBlockWebViewClient : WebViewClient() {
    private val mAdBlockEnabled: Boolean = false
    private val mLoadedUrls: Map<String, Boolean> = new HashMap<>()

    constructor(adBlockEnabled: Boolean) {
        mAdBlockEnabled = adBlockEnabled
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse {
        if (!mAdBlockEnabled) {
            return super.shouldInterceptRequest(view, url)
        }
        boolean ad
        if (!mLoadedUrls.containsKey(url)) {
            ad = AdBlocker.isAd(url)
            mLoadedUrls.put(url, ad)
        } else {
            ad = mLoadedUrls.get(url)
        }
        return ad ? AdBlocker.createEmptyResource() :
                super.shouldInterceptRequest(view, url)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
        if (!mAdBlockEnabled) {
            return super.shouldInterceptRequest(view, request)
        }
        boolean ad
        String url = request.getUrl().toString()
        if (!mLoadedUrls.containsKey(url)) {
            ad = AdBlocker.isAd(url)
            mLoadedUrls.put(url, ad)
        } else {
            ad = mLoadedUrls.get(url)
        }
        return ad ? AdBlocker.createEmptyResource() :
                super.shouldInterceptRequest(view, request)
    }
}
