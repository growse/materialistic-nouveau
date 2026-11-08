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
package com.growse.android.io.github.hidroh.materialistic.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.WebView
import com.growse.android.io.github.hidroh.materialistic.Preferences.adBlockEnabled
import com.growse.android.io.github.hidroh.materialistic.widget.AdBlockWebViewClient
import com.growse.android.io.github.hidroh.materialistic.widget.CacheableWebView
import com.growse.android.io.github.hidroh.materialistic.widget.CacheableWebView.ArchiveClient

class WebCacheService : Service() {
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) { // restarted
      stopSelf(startId)
      return START_STICKY
    }
    val webView = CacheableWebView(this)
    webView.webViewClient = AdBlockWebViewClient(adBlockEnabled(this))
    webView.setWebChromeClient(
        object : ArchiveClient() {
          override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress == 100) {
              stopSelf(startId)
            }
          }
        },
    )
    webView.loadUrl(intent.getStringExtra(EXTRA_URL)!!)
    return START_STICKY
  }

  companion object {
    const val EXTRA_URL: String = "extra:url"
  }
}
