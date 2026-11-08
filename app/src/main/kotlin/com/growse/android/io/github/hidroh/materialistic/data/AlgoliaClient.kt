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
import com.growse.android.io.github.hidroh.materialistic.di.DataModule
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable
import rx.Scheduler
import rx.functions.Action1
import rx.functions.Func1

open class AlgoliaClient @Inject constructor(factory: RestServiceFactory) : ItemManager {
  var mRestService: RestService

  @JvmField @Inject @Named(ItemManagerQualifiers.HN) var mHackerNewsClient: ItemManager? = null

  @JvmField @Inject @Named(DataModule.MAIN_THREAD) var mMainThreadScheduler: Scheduler? = null

  init {
    mRestService = factory.rxEnabled(true).create(BASE_API_URL, RestService::class.java)
  }

  override fun getStories(
      filter: String,
      @ItemManager.CacheMode cacheMode: Int,
      listener: ResponseListener<Array<Item?>?>?,
  ) {
    if (listener == null) {
      return
    }
    searchRx(filter)!!
        .map(Func1 { algoliaHits: AlgoliaHits? -> this.toItems(algoliaHits) })
        .observeOn(mMainThreadScheduler)
        .subscribe(
            Action1 { response: Array<Item?>? -> listener.onResponse(response) },
        ) { t: Throwable? ->
          listener.onError(if (t != null) t.message else "")
        }
  }

  override fun getItem(
      itemId: String,
      @ItemManager.CacheMode cacheMode: Int,
      listener: ResponseListener<Item?>?,
  ) {
    mHackerNewsClient!!.getItem(itemId, cacheMode, listener)
  }

  override fun getStories(filter: String, @ItemManager.CacheMode cacheMode: Int): Array<Item?>? {
    return try {
      toItems(search(filter)!!.execute().body())
    } catch (_: IOException) {
      arrayOfNulls(0)
    }
  }

  override fun getItem(itemId: String, @ItemManager.CacheMode cacheMode: Int): Item? {
    return mHackerNewsClient!!.getItem(itemId, cacheMode)
  }

  protected open fun searchRx(filter: String): Observable<AlgoliaHits?>? {
    // TODO add ETag header
    return if (sSortByTime) mRestService.searchByDateRx(filter) else mRestService.searchRx(filter)
  }

  protected open fun search(filter: String): Call<AlgoliaHits?>? {
    return if (sSortByTime) mRestService.searchByDate(filter) else mRestService.search(filter)
  }

  private fun toItems(algoliaHits: AlgoliaHits?): Array<Item?> {
    if (algoliaHits == null) {
      return arrayOfNulls(0)
    }
    val hits = algoliaHits.hits
    val stories = arrayOfNulls<Item>(hits?.size ?: 0)
    for (i in stories.indices) {
      val item = HackerNewsItem(hits!![i]!!.objectID!!.toLong())
      item.rank = i + 1
      stories[i] = item
    }
    return stories
  }

  interface RestService {
    @GET(
        "search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun searchByDateRx(@Query("query") query: String?): Observable<AlgoliaHits?>?

    @GET(
        "search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun searchRx(@Query("query") query: String?): Observable<AlgoliaHits?>?

    @GET(
        "search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun searchByMinTimestampRx(
        @Query("numericFilters") timestampSeconds: String?,
    ): Observable<AlgoliaHits?>?

    @GET(
        "search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun searchByDate(@Query("query") query: String?): Call<AlgoliaHits?>?

    @GET(
        "search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun search(@Query("query") query: String?): Call<AlgoliaHits?>?

    @GET(
        "search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none",
    )
    fun searchByMinTimestamp(
        @Query("numericFilters") timestampSeconds: String?,
    ): Call<AlgoliaHits?>?
  }

  class AlgoliaHits {
    @Keep var hits: Array<Hit?>? = null
  }

  class Hit {
    @Keep var objectID: String? = null
  }

  companion object {
    var sSortByTime: Boolean = true
    const val HOST: String = "hn.algolia.com"
    private const val BASE_API_URL = "https://$HOST/api/v1/"
    const val MIN_CREATED_AT: String = "created_at_i>"
  }
}
