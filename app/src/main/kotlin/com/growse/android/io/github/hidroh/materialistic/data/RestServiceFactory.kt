/*
 * Copyright (c) 2015 Ha Duy Trung
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

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import javax.inject.Inject
import okhttp3.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.schedulers.Schedulers

interface RestServiceFactory {
  fun rxEnabled(enabled: Boolean): RestServiceFactory

  fun <T> create(baseUrl: String, clazz: Class<T>): T

  fun <T> create(baseUrl: String, clazz: Class<T>, callbackExecutor: Executor?): T

  class Impl @Inject constructor(private val mCallFactory: Call.Factory) : RestServiceFactory {
    private var mRxEnabled = false

    override fun rxEnabled(enabled: Boolean): RestServiceFactory {
      mRxEnabled = enabled
      return this
    }

    override fun <T> create(baseUrl: String, clazz: Class<T>): T = create(baseUrl, clazz, null)

    override fun <T> create(
        baseUrl: String,
        clazz: Class<T>,
        callbackExecutor: Executor?,
    ): T {
      val builder = Retrofit.Builder()
      if (mRxEnabled) {
        builder.addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
      }
      builder.callFactory(mCallFactory).callbackExecutor(callbackExecutor ?: MainThreadExecutor())
      return builder
          .baseUrl(baseUrl)
          .addConverterFactory(GsonConverterFactory.create())
          .build()
          .create(clazz)
    }
  }

  class MainThreadExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())

    override fun execute(r: Runnable) {
      handler.post(r)
    }
  }

  companion object {
    const val CACHE_CONTROL_FORCE_CACHE =
        "Cache-Control: only-if-cached, max-stale=${Int.MAX_VALUE}"
    const val CACHE_CONTROL_FORCE_NETWORK = "Cache-Control: no-cache"
    const val CACHE_CONTROL_MAX_AGE_30M = "Cache-Control: max-age=${30 * 60}"
    const val CACHE_CONTROL_MAX_AGE_24H = "Cache-Control: max-age=${24 * 60 * 60}"
  }
}
