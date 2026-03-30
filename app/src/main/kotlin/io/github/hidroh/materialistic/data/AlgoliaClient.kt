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

import java.io.IOException

import javax.inject.Inject
import javax.inject.Named

import androidx.annotation.Keep
import androidx.annotation.NonNull
import io.github.hidroh.materialistic.ActivityModule
import io.github.hidroh.materialistic.DataModule
import io.github.hidroh.materialistic.annotation.Synthetic
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable
import rx.Scheduler

open class AlgoliaClient : ItemManager {

    var sSortByTime: Boolean = true
    const val HOST: String = "hn.algolia.com"
    private const val BASE_API_URL: String = "https://" + HOST + "/api/v1/"
    const val MIN_CREATED_AT: String = "created_at_i>"
    var mRestService: RestService? = null
    @Inject @Named(ActivityModule.HN) var mHackerNewsClient: ItemManager? = null
    @Inject @Named(DataModule.MAIN_THREAD) var mMainThreadScheduler: Scheduler? = null

    @Inject
    constructor(factory: RestServiceFactory) {
        mRestService = factory.rxEnabled(true).create(BASE_API_URL, RestService::class.java)
    }

    override fun getStories(filter: String, cacheMode: Int, listener: ResponseListener<Array<Item>>) {
        if (listener == null) {
            return
        }
        searchRx(filter)
                .map(this::toItems)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""))
    }

    override fun getItem(itemId: String, cacheMode: Int, listener: ResponseListener<Item>) {
        mHackerNewsClient.getItem(itemId, cacheMode, listener)
    }

    override fun getStories(filter: String, cacheMode: Int): Array<Item> {
        try {
            return toItems(search(filter).execute().body())
        } catch (IOException e) {
            return Item[0]
        }
    }

    override fun getItem(itemId: String, cacheMode: Int): Item {
        return mHackerNewsClient.getItem(itemId, cacheMode)
    }

    protected fun searchRx(filter: String): Observable<AlgoliaHits> {
        // TODO add ETag header
        return sSortByTime ? mRestService.searchByDateRx(filter) : mRestService.searchRx(filter)
    }

    protected fun search(filter: String): Call<AlgoliaHits> {
        return sSortByTime ? mRestService.searchByDate(filter) : mRestService.search(filter)
    }

    @NonNull
    private fun toItems(algoliaHits: AlgoliaHits): Array<Item> {
        if (algoliaHits == null) {
            return Item[0]
        }
        val hits = algoliaHits.hits
        val stories = Item[hits == null ? 0 : hits.length]
        for (int i = 0; i < stories.length; i++) {
            //noinspection ConstantConditions
            val item = HackerNewsItem(
                    Long.parseLong(hits[i].objectID))
            item.rank = i + 1
            stories[i] = item
        }
        return stories
    }

    interface RestService {
        @GET("search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun searchByDateRx(query: String): Observable<AlgoliaHits>

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun searchRx(query: String): Observable<AlgoliaHits>

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun searchByMinTimestampRx(timestampSeconds: String): Observable<AlgoliaHits>

        @GET("search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun searchByDate(query: String): Call<AlgoliaHits>

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun search(query: String): Call<AlgoliaHits>

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        fun searchByMinTimestamp(timestampSeconds: String): Call<AlgoliaHits>
    }

    open class AlgoliaHits {
        @Keep @Synthetic
        var hits: Array<Hit>? = null
    }

    open class Hit {
        @Keep @Synthetic
        var objectID: String? = null
    }
}
