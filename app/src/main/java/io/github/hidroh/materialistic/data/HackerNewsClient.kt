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

import androidx.annotation.NonNull

import java.io.IOException

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.DataModule
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import rx.Observable
import rx.Scheduler

/**
 * Client to retrieve Hacker News content asynchronously
 */
open class HackerNewsClient : ItemManager, UserManager {
    const val HOST: String = "hacker-news.firebaseio.com"
    const val BASE_WEB_URL: String = "https://news.ycombinator.com"
    const val WEB_ITEM_PATH: String = BASE_WEB_URL + "/item?id=%s"
    const val BASE_API_URL: String = "https://" + HOST + "/v0/"
    @Inject @Named(DataModule.IO_THREAD) var mIoScheduler: Scheduler? = null
    @Inject @Named(DataModule.MAIN_THREAD) var mMainThreadScheduler: Scheduler? = null
    private var mRestService: RestService? = null
    private var mSessionManager: SessionManager? = null
    private var mFavoriteManager: FavoriteManager? = null

    @Inject
    constructor(factory: RestServiceFactory, sessionManager: SessionManager, favoriteManager: FavoriteManager) {
        mRestService = factory.rxEnabled(true).create(BASE_API_URL, RestService::class.java)
        mSessionManager = sessionManager
        mFavoriteManager = favoriteManager
    }

    override fun getStories(filter: String, cacheMode: Int, listener: ResponseListener<Array<Item>>) {
        if (listener == null) {
            return
        }
        Observable.defer(() -> getStoriesObservable(filter, cacheMode))
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""))
    }

    override fun getItem(itemId: String, cacheMode: Int, listener: ResponseListener<Item>) {
        if (listener == null) {
            return
        }
        Observable<HackerNewsItem> itemObservable
        switch (cacheMode) {
            case MODE_DEFAULT:
            default:
                itemObservable = mRestService.itemRx(itemId)
                break
            case MODE_NETWORK:
                itemObservable = mRestService.networkItemRx(itemId)
                break
            case MODE_CACHE:
                itemObservable = mRestService.cachedItemRx(itemId)
                        .onErrorResumeNext(mRestService.itemRx(itemId))
                break
        }
        Observable.defer(() -> Observable.zip(
                mSessionManager.isViewed(itemId),
                mFavoriteManager.check(itemId),
                itemObservable,
                (isViewed, favorite, hackerNewsItem) -> {
                    if (hackerNewsItem != null) {
                        hackerNewsItem.preload()
                        hackerNewsItem.setIsViewed(isViewed)
                        hackerNewsItem.setFavorite(favorite)
                    }
                    return hackerNewsItem
                }))
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""))

    }

    override fun getStories(filter: String, cacheMode: Int): Array<Item> {
        try {
            return toItems(getStoriesCall(filter, cacheMode).execute().body())
        } catch (IOException e) {
            return Item[0]
        }
    }

    override fun getItem(itemId: String, cacheMode: Int): Item {
        Call<HackerNewsItem> call
        switch (cacheMode) {
            case MODE_DEFAULT:
            case MODE_CACHE:
            default:
                call = mRestService.item(itemId)
                break
            case MODE_NETWORK:
                call = mRestService.networkItem(itemId)
                break
        }
        try {
            return call.execute().body()
        } catch (IOException e) {
            return null
        }
    }

    override fun getUser(username: String, listener: ResponseListener<User>) {
        if (listener == null) {
            return
        }
        mRestService.userRx(username)
                .map(userItem -> {
                    if (userItem != null) {
                        userItem.setSubmittedItems(toItems(userItem.getSubmitted()))
                    }
                    return userItem
                })
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""))
    }

    @NonNull
    private fun getStoriesObservable(filter: String, cacheMode: Int): Observable<Array<Item>> {
        Observable<int[]> observable
        switch (filter) {
            case NEW_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkNewStoriesRx() : mRestService.newStoriesRx()
                break
            case SHOW_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkShowStoriesRx() : mRestService.showStoriesRx()
                break
            case ASK_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkAskStoriesRx() : mRestService.askStoriesRx()
                break
            case JOBS_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkJobStoriesRx() : mRestService.jobStoriesRx()
                break
            case BEST_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkBestStoriesRx() : mRestService.bestStoriesRx()
                break
            default:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkTopStoriesRx() : mRestService.topStoriesRx()
                break
        }
        return observable.map(this::toItems)
    }

    @NonNull
    private fun getStoriesCall(filter: String, cacheMode: Int): Call<IntArray> {
        Call<int[]> call
        if (filter == null) {
            // for legacy 'new stories' widgets
            return cacheMode == MODE_NETWORK ?
                    mRestService.networkNewStories() : mRestService.newStories()
        }
        switch (filter) {
            case NEW_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkNewStories() : mRestService.newStories()
                break
            case SHOW_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkShowStories() : mRestService.showStories()
                break
            case ASK_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkAskStories() : mRestService.askStories()
                break
            case JOBS_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkJobStories() : mRestService.jobStories()
                break
            case BEST_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkBestStories() : mRestService.bestStories()
                break
            default:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkTopStories() : mRestService.topStories()
                break
        }
        return call
    }

    private fun toItems(ids: IntArray): Array<HackerNewsItem> {
        if (ids == null) {
            return null
        }
        HackerNewsItem[] items = HackerNewsItem[ids.length]
        for (int i = 0; i < items.length; i++) {
            HackerNewsItem item = HackerNewsItem(ids[i])
            item.rank = i + 1
            items[i] = item
        }
        return items
    }

    interface RestService {
        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("topstories.json")
        fun topStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("newstories.json")
        fun newStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("showstories.json")
        fun showStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("askstories.json")
        fun askStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("jobstories.json")
        fun jobStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("beststories.json")
        fun bestStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("topstories.json")
        fun networkTopStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("newstories.json")
        fun networkNewStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("showstories.json")
        fun networkShowStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("askstories.json")
        fun networkAskStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("jobstories.json")
        fun networkJobStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("beststories.json")
        fun networkBestStoriesRx(): Observable<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("item/{itemId}.json")
        fun itemRx(itemId: String): Observable<HackerNewsItem>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("item/{itemId}.json")
        fun networkItemRx(itemId: String): Observable<HackerNewsItem>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
        @GET("item/{itemId}.json")
        fun cachedItemRx(itemId: String): Observable<HackerNewsItem>

        @GET("user/{userId}.json")
        fun userRx(userId: String): Observable<UserItem>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("topstories.json")
        fun topStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("newstories.json")
        fun newStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("showstories.json")
        fun showStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("askstories.json")
        fun askStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("jobstories.json")
        fun jobStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("beststories.json")
        fun bestStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("topstories.json")
        fun networkTopStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("newstories.json")
        fun networkNewStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("showstories.json")
        fun networkShowStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("askstories.json")
        fun networkAskStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("jobstories.json")
        fun networkJobStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("beststories.json")
        fun networkBestStories(): Call<IntArray>

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("item/{itemId}.json")
        fun item(itemId: String): Call<HackerNewsItem>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("item/{itemId}.json")
        fun networkItem(itemId: String): Call<HackerNewsItem>

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
        @GET("item/{itemId}.json")
        fun cachedItem(itemId: String): Call<HackerNewsItem>

        @GET("user/{userId}.json")
        fun user(userId: String): Call<UserItem>
    }
}
