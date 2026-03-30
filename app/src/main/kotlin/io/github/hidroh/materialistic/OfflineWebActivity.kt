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

package io.github.hidroh.materialistic

import android.graphics.Color
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar

import io.github.hidroh.materialistic.widget.AdBlockWebViewClient
import io.github.hidroh.materialistic.widget.CacheableWebView

open class OfflineWebActivity : InjectableActivity() {
    val EXTRA_URL: String = OfflineWebActivity::class.java.name + ".EXTRA_URL"

    @SuppressWarnings("ConstantConditions")
    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val url = getIntent().getStringExtra(EXTRA_URL)
        if (TextUtils.isEmpty(url)) {
            finish()
            return
        }
        setTitle(url)
        setContentView(R.layout.activity_offline_web)
        val scrollView = (NestedScrollView) findViewById(R.id.nested_scroll_view)
        val toolbar = (Toolbar) findViewById(R.id.toolbar)
        toolbar.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0))
        setSupportActionBar(toolbar)
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE)
        getSupportActionBar().setSubtitle(R.string.offline)
        val progressBar = (ProgressBar) findViewById(R.id.progress)
        val webView = (WebView) findViewById(R.id.web_view)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setWebViewClient(AdBlockWebViewClient(Preferences.adBlockEnabled(this)) {
            public void onPageFinished(WebView view, String url) {
                setTitle(view.getTitle())
            }
        })
        webView.setWebChromeClient(CacheableWebView.ArchiveClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress)
                progressBar.setVisibility(View.VISIBLE)
                progressBar.setProgress(newProgress)
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE)
                    webView.setBackgroundColor(Color.WHITE)
                    webView.setVisibility(View.VISIBLE)
                }
            }
        })
        AppUtils.toggleWebViewZoom(webView.getSettings(), true)
        webView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
