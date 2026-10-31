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
import android.text.TextUtils
import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.Callable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.source
import rx.Observable
import rx.Scheduler
import rx.functions.Func1

object AdBlocker {
  private const val AD_HOSTS_FILE = "pgl.yoyo.org.txt"
  private val AD_HOSTS: MutableSet<String?> = HashSet()

  fun init(context: Context, scheduler: Scheduler?) {
    Observable.fromCallable<Void?>(Callable { loadFromAssets(context) })
        .onErrorReturn(Func1 { null })
        .subscribeOn(scheduler)
        .subscribe()
  }

  fun isAd(url: String): Boolean {
    val httpUrl: HttpUrl? = url.toHttpUrlOrNull()
    return isAdHost(httpUrl?.host ?: "")
  }

  fun createEmptyResource(): WebResourceResponse {
    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
  }

  @WorkerThread
  @Throws(IOException::class)
  private fun loadFromAssets(context: Context): Void? {
    val stream = context.assets.open(AD_HOSTS_FILE)
    val buffer = stream.source().buffer()
    var line: String?
    while ((buffer.readUtf8Line().also { line = it }) != null) {
      AD_HOSTS.add(line)
    }
    buffer.close()
    stream.close()
    return null
  }

  /**
   * Recursively walking up subdomain chain until we exhaust or find a match, effectively doing a
   * longest substring matching here
   */
  private fun isAdHost(host: String?): Boolean {
    if (TextUtils.isEmpty(host)) {
      return false
    }
    val index = host!!.indexOf(".")
    return index >= 0 &&
        (AD_HOSTS.contains(host) || index + 1 < host.length && isAdHost(host.substring(index + 1)))
  }
}
