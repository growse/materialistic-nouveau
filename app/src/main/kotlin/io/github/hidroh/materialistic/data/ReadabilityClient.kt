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

import androidx.annotation.Keep
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.AndroidUtils
import io.github.hidroh.materialistic.BuildConfig
import io.github.hidroh.materialistic.DataModule
import io.github.hidroh.materialistic.annotation.Synthetic
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers

interface ReadabilityClient {
    var HOST: String = "mercury.postlight.com"

    interface Callback {
        fun onResponse(content: String)
    }

    fun parse(itemId: String, url: String, callback: Callback)

    @WorkerThread
    fun parse(itemId: String, url: String)

    open class Impl : ReadabilityClient {
        private const val EMPTY_CONTENT: CharSequence = "<div></div>"
        private var mMercuryService: MercuryService? = null
        private var mCache: LocalCache? = null
        @Inject @Named(DataModule.IO_THREAD) var mIoScheduler: Scheduler? = null
        @Inject @Named(DataModule.MAIN_THREAD) var mMainThreadScheduler: Scheduler? = null

        interface MercuryService {
            var MERCURY_API_URL: String = "https://" + HOST + "/"
            var X_API_KEY: String = "x-api-key: "

            @Headers({RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H,
            @GET("parser")
            Observable<Readable> parse(@Query("url") String url)
        }

        open class Readable {
            @Keep @Synthetic
            var content: String? = null
        }

        @Inject
        fun Impl(cache: LocalCache, factory: RestServiceFactory) {
            mMercuryService = factory.rxEnabled(true)
                    .create(MercuryService.MERCURY_API_URL,
                            MercuryService::class.java)
            mCache = cache
        }

        override fun parse(itemId: String, url: String, callback: Callback) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(mIoScheduler)
                    .flatMap(content -> content != null ?
                            Observable.just(content) : fromNetwork(itemId, url))
                    .map(content -> AndroidUtils.TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(mMainThreadScheduler)
                    .subscribe(callback::onResponse)
        }

        @WorkerThread
        override fun parse(itemId: String, url: String) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(Schedulers.immediate())
                    .switchIfEmpty(fromNetwork(itemId, url))
                    .map(content -> AndroidUtils.TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(Schedulers.immediate())
                    .subscribe()
        }

        @NonNull
        private fun fromNetwork(itemId: String, url: String): Observable<String> {
            return mMercuryService.parse(url)
                    .onErrorReturn(throwable -> null)
                    .map(readable -> readable == null ? null : readable.content)
                    .doOnNext(content -> mCache.putReadability(itemId, content))
        }

        private fun fromCache(itemId: String): Observable<String> {
            return Observable.just(mCache.getReadability(itemId))
        }
    }
}
