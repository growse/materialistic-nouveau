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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import android.text.TextUtils
import android.webkit.WebResourceResponse

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.HashSet
import java.util.Set

import okhttp3.HttpUrl
import okio.BufferedSource
import okio.Okio
import rx.Observable
import rx.Scheduler

open class AdBlocker {
    private const val AD_HOSTS_FILE: String = "pgl.yoyo.org.txt"
    private const val AD_HOSTS: Set<String> = new HashSet<>()

    fun init(context: Context, scheduler: Scheduler) {
        Observable.fromCallable(() -> loadFromAssets(context))
                .onErrorReturn(throwable -> null)
                .subscribeOn(scheduler)
                .subscribe()
    }

    fun isAd(url: String): Boolean {
        val httpUrl = HttpUrl.parse(url)
        return isAdHost(httpUrl != null ? httpUrl.host() : "")
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".getBytes()))
    }

    @WorkerThread
    private fun loadFromAssets(context: Context): Void {
        val stream = context.getAssets().open(AD_HOSTS_FILE)
        val buffer = Okio.buffer(Okio.source(stream))
        String line
        while ((line = buffer.readUtf8Line()) != null) {
            AD_HOSTS.add(line)
        }
        buffer.close()
        stream.close()
        return null
    }

    /**
     * Recursively walking up sub domain chain until we exhaust or find a match,
     * effectively doing a longest substring matching here
     */
    private fun isAdHost(host: String): Boolean {
        if (TextUtils.isEmpty(host)) {
            return false
        }
        int index = host.indexOf(".")
        return index >= 0 && (AD_HOSTS.contains(host) ||
                index + 1 < host.length() && isAdHost(host.substring(index + 1)))
    }
}
