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

import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.growse.android.io.github.hidroh.materialistic.BuildConfig
import com.growse.android.io.github.hidroh.materialistic.di.DataModule
import javax.inject.Inject
import javax.inject.Named
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import rx.Observable
import rx.Scheduler
import rx.functions.Action1
import rx.functions.Func1
import rx.schedulers.Schedulers

interface ReadabilityClient {
  fun interface Callback {
    fun onResponse(content: String?)
  }

  fun parse(itemId: String?, url: String?, callback: Callback)

  @WorkerThread fun parse(itemId: String?, url: String?)

  class Impl
  @Inject
  constructor(
      private val cache: LocalCache,
      factory: RestServiceFactory,
      @param:Named(DataModule.IO_THREAD) private val ioScheduler: Scheduler,
      @param:Named(DataModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler,
  ) : ReadabilityClient {
    private val mercuryService: MercuryService

    internal interface MercuryService {
      @Headers(
          RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H,
          X_API_KEY + BuildConfig.MERCURY_TOKEN,
      )
      @GET("parser")
      fun parse(@Query("url") url: String?): Observable<Readable?>?

      companion object {
        const val MERCURY_API_URL: String = "https://$HOST/"
        const val X_API_KEY: String = "x-api-key: "
      }
    }

    internal class Readable {
      @Keep var content: String? = null
    }

    init {
      mercuryService =
          factory
              .rxEnabled(true)
              .create(
                  MercuryService.MERCURY_API_URL,
                  MercuryService::class.java,
              )
    }

    override fun parse(itemId: String?, url: String?, callback: Callback) {
      Observable.defer<String?> { fromCache(itemId) }
          .subscribeOn(ioScheduler)
          .flatMap(
              Func1 { content: String? ->
                if (content != null) Observable.just(content) else fromNetwork(itemId, url)
              },
          )
          .map<String?>(
              Func1 { content: String? -> if (content == EMPTY_CONTENT) null else content },
          )
          .observeOn(mainThreadScheduler)
          .subscribe(Action1 { content: String? -> callback.onResponse(content) })
    }

    @WorkerThread
    override fun parse(itemId: String?, url: String?) {
      Observable.defer<String?> { fromCache(itemId) }
          .subscribeOn(Schedulers.immediate())
          .switchIfEmpty(fromNetwork(itemId, url))
          .map<String?>(
              Func1 { content: String? -> if (content == EMPTY_CONTENT) null else content },
          )
          .observeOn(Schedulers.immediate())
          .subscribe()
    }

    private fun fromNetwork(itemId: String?, url: String?): Observable<String?> {
      return mercuryService
          .parse(url)!!
          .onErrorReturn(Func1 { null })
          .map<String?>(Func1 { readable: Readable? -> readable?.content })
          .doOnNext(Action1 { content: String? -> cache.putReadability(itemId, content) })
    }

    private fun fromCache(itemId: String?): Observable<String?> {
      return Observable.just<String?>(cache.getReadability(itemId))
    }

    companion object {
      private const val EMPTY_CONTENT = "<div></div>"
    }
  }

  companion object {
    const val HOST: String = "mercury.postlight.com"
  }
}
