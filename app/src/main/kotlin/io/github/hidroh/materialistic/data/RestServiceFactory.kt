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

package io.github.hidroh.materialistic.data

import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull

import java.util.concurrent.Executor

import javax.inject.Inject

import okhttp3.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.schedulers.Schedulers

interface RestServiceFactory {
    var CACHE_CONTROL_FORCE_CACHE: String = "Cache-Control: only-if-cached, max-stale=" + Integer.MAX_VALUE
    var CACHE_CONTROL_FORCE_NETWORK: String = "Cache-Control: no-cache"
    var CACHE_CONTROL_MAX_AGE_30M: String = "Cache-Control: max-age=" + (30 * 60)
    var CACHE_CONTROL_MAX_AGE_24H: String = "Cache-Control: max-age=" + (24 * 60 * 60)

    fun rxEnabled(enabled: Boolean): RestServiceFactory

    fun create(baseUrl: String, clazz: Class<T>): <T> T

    fun create(baseUrl: String, clazz: Class<T>, callbackExecutor: Executor): <T> T

    open class Impl : RestServiceFactory {
        private var mCallFactory: Call.Factory? = null
        private var mRxEnabled: Boolean = false

        @Inject
        constructor(callFactory: Call.Factory) {
            this.mCallFactory = callFactory
        }

        override fun rxEnabled(enabled: Boolean): RestServiceFactory {
            mRxEnabled = enabled
            return this
        }

        override fun create(baseUrl: String, clazz: Class<T>): <T> T {
            return create(baseUrl, clazz, null)
        }

        override fun create(baseUrl: String, clazz: Class<T>, callbackExecutor: Executor): <T> T {
            Retrofit.Builder builder = Retrofit.Builder()
            if (mRxEnabled) {
                builder.addCallAdapterFactory(RxJavaCallAdapterFactory
                        .createWithScheduler(Schedulers.io()))
            }
            builder.callFactory(mCallFactory)
                    .callbackExecutor(callbackExecutor != null ?
                            callbackExecutor : MainThreadExecutor())
            return builder.baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(clazz)
        }
    }

    open class MainThreadExecutor : Executor {
        private val handler: Handler = new Handler(Looper.getMainLooper())

        fun execute(r: Runnable) {
            handler.post(r)
        }
    }
}
