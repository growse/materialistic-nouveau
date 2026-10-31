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

import com.growse.android.io.github.hidroh.materialistic.data.ItemManager.FetchMode
import com.growse.android.io.github.hidroh.materialistic.data.RestServiceFactory.Companion.CACHE_CONTROL_MAX_AGE_30M
import com.growse.android.io.github.hidroh.materialistic.di.DataModule
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import rx.Observable
import rx.Scheduler

/** Client to retrieve Hacker News content asynchronously */
class HackerNewsClient
@Inject
constructor(
    factory: RestServiceFactory,
    private val sessionManager: SessionManager,
    private val favoriteManager: FavoriteManager,
    private val localCache: LocalCache,
    @param:Named(DataModule.IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(DataModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler,
) : ItemManager, UserManager {
  private val restService: RestService =
      factory.rxEnabled(true).create(BASE_API_URL, RestService::class.java)

  override fun getStories(
      @FetchMode filter: String,
      @ItemManager.CacheMode cacheMode: Int,
      listener: ResponseListener<Array<Item?>?>?,
  ) {
    if (listener == null) {
      return
    }
    Observable.defer { getStoriesObservable(filter, cacheMode) }
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({ listener.onResponse(it) }, { listener.onError(it?.message ?: "") })
  }

  override fun getItem(
      itemId: String,
      @ItemManager.CacheMode cacheMode: Int,
      listener: ResponseListener<Item?>?,
  ) {
    if (listener == null) {
      return
    }
    val itemObservable: Observable<HackerNewsItem?> =
        when (cacheMode) {
          ItemManager.MODE_NETWORK -> restService.networkItemRx(itemId).doOnNext { cacheItem(it) }
          ItemManager.MODE_CACHE ->
              Observable.defer {
                localCache.getCachedItem(itemId)?.let {
                  return@defer Observable.just(HackerNewsItem.fromCachedItem(it))
                }
                restService
                    .cachedItemRx(itemId)
                    .onErrorResumeNext(restService.itemRx(itemId))
                    .doOnNext { cacheItem(it) }
              }
          else ->
              Observable.defer {
                val cached = localCache.getCachedItem(itemId)
                if (cached != null && !isStale(cached)) {
                  return@defer Observable.just(HackerNewsItem.fromCachedItem(cached))
                }
                restService.itemRx(itemId).doOnNext { cacheItem(it) }
              }
        }
    Observable.defer {
          Observable.zip(
              sessionManager.isViewed(itemId),
              favoriteManager.check(itemId),
              itemObservable,
          ) { isViewed, favorite, item ->
            item?.apply {
              preload()
              this.isViewed = isViewed
              isFavorite = favorite
            }
          }
        }
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({ listener.onResponse(it) }, { listener.onError(it?.message ?: "") })
  }

  override fun getStories(filter: String, @ItemManager.CacheMode cacheMode: Int): Array<Item?>? =
      try {
        getStoriesCall(filter, cacheMode).execute().body()?.let { toItems(it).toTypedArray() }
      } catch (_: IOException) {
        emptyArray()
      }

  override fun getItem(itemId: String, @ItemManager.CacheMode cacheMode: Int): Item? {
    if (cacheMode != ItemManager.MODE_NETWORK) {
      val cached = localCache.getCachedItem(itemId)
      if (cached != null && (cacheMode == ItemManager.MODE_CACHE || !isStale(cached))) {
        return HackerNewsItem.fromCachedItem(cached)
      }
    }
    val call =
        if (cacheMode == ItemManager.MODE_NETWORK) restService.networkItem(itemId)
        else restService.item(itemId)
    return try {
      call.execute().body().also { cacheItem(it) }
    } catch (_: IOException) {
      null
    }
  }

  private fun isStale(cached: MaterialisticDatabase.CachedItem): Boolean =
      System.currentTimeMillis() - cached.fetchedAt > MUTABLE_FIELD_TTL_MILLIS

  private fun cacheItem(item: HackerNewsItem?) {
    if (item != null) {
      localCache.putCachedItem(HackerNewsItem.toCachedItem(item))
    }
  }

  override fun getUser(username: String?, listener: ResponseListener<UserManager.User?>?) {
    if (listener == null) {
      return
    }
    restService
        .userRx(username)
        .map { userItem ->
          userItem?.setSubmittedItems(userItem.submitted?.let { toItems(it).toTypedArray() })
          userItem
        }
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({ listener.onResponse(it) }, { listener.onError(it?.message ?: "") })
  }

  private fun getStoriesObservable(
      @FetchMode filter: String,
      @ItemManager.CacheMode cacheMode: Int,
  ): Observable<Array<Item?>?> {
    val network = cacheMode == ItemManager.MODE_NETWORK
    val observable =
        when (filter) {
          ItemManager.NEW_FETCH_MODE ->
              if (network) restService.networkNewStoriesRx() else restService.newStoriesRx()
          ItemManager.SHOW_FETCH_MODE ->
              if (network) restService.networkShowStoriesRx() else restService.showStoriesRx()
          ItemManager.ASK_FETCH_MODE ->
              if (network) restService.networkAskStoriesRx() else restService.askStoriesRx()
          ItemManager.JOBS_FETCH_MODE ->
              if (network) restService.networkJobStoriesRx() else restService.jobStoriesRx()
          ItemManager.BEST_FETCH_MODE ->
              if (network) restService.networkBestStoriesRx() else restService.bestStoriesRx()
          else -> if (network) restService.networkTopStoriesRx() else restService.topStoriesRx()
        }
    return observable.map { ids -> ids?.let { toItems(it).toTypedArray<Item?>() } }
  }

  private fun getStoriesCall(
      @FetchMode filter: String,
      @ItemManager.CacheMode cacheMode: Int,
  ): Call<IntArray?> {
    val network = cacheMode == ItemManager.MODE_NETWORK
    return when (filter) {
      ItemManager.NEW_FETCH_MODE ->
          if (network) restService.networkNewStories() else restService.newStories()
      ItemManager.SHOW_FETCH_MODE ->
          if (network) restService.networkShowStories() else restService.showStories()
      ItemManager.ASK_FETCH_MODE ->
          if (network) restService.networkAskStories() else restService.askStories()
      ItemManager.JOBS_FETCH_MODE ->
          if (network) restService.networkJobStories() else restService.jobStories()
      ItemManager.BEST_FETCH_MODE ->
          if (network) restService.networkBestStories() else restService.bestStories()
      else -> if (network) restService.networkTopStories() else restService.topStories()
    }
  }

  /** Creates unpopulated items for [ids], ranked by their position. */
  private fun toItems(ids: IntArray): List<HackerNewsItem> = ids.mapIndexed { i, id ->
    HackerNewsItem(id.toLong()).apply { rank = i + 1 }
  }

  internal interface RestService {
    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("topstories.json")
    fun topStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("newstories.json")
    fun newStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("showstories.json")
    fun showStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("askstories.json")
    fun askStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("jobstories.json")
    fun jobStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("beststories.json")
    fun bestStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("topstories.json")
    fun networkTopStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("newstories.json")
    fun networkNewStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("showstories.json")
    fun networkShowStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("askstories.json")
    fun networkAskStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("jobstories.json")
    fun networkJobStoriesRx(): Observable<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("beststories.json")
    fun networkBestStoriesRx(): Observable<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("item/{itemId}.json")
    fun itemRx(@Path("itemId") itemId: String?): Observable<HackerNewsItem?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("item/{itemId}.json")
    fun networkItemRx(@Path("itemId") itemId: String?): Observable<HackerNewsItem?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
    @GET("item/{itemId}.json")
    fun cachedItemRx(@Path("itemId") itemId: String?): Observable<HackerNewsItem?>

    @GET("user/{userId}.json") fun userRx(@Path("userId") userId: String?): Observable<UserItem?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("topstories.json") fun topStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("newstories.json") fun newStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("showstories.json") fun showStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("askstories.json") fun askStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("jobstories.json") fun jobStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M) @GET("beststories.json") fun bestStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("topstories.json")
    fun networkTopStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("newstories.json")
    fun networkNewStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("showstories.json")
    fun networkShowStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("askstories.json")
    fun networkAskStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("jobstories.json")
    fun networkJobStories(): Call<IntArray?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("beststories.json")
    fun networkBestStories(): Call<IntArray?>

    @Headers(CACHE_CONTROL_MAX_AGE_30M)
    @GET("item/{itemId}.json")
    fun item(@Path("itemId") itemId: String?): Call<HackerNewsItem?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
    @GET("item/{itemId}.json")
    fun networkItem(@Path("itemId") itemId: String?): Call<HackerNewsItem?>

    @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
    @GET("item/{itemId}.json")
    fun cachedItem(@Path("itemId") itemId: String?): Call<HackerNewsItem?>

    @GET("user/{userId}.json") fun user(@Path("userId") userId: String?): Call<UserItem?>
  }

  companion object {
    const val HOST: String = "hacker-news.firebaseio.com"
    const val BASE_WEB_URL: String = "https://news.ycombinator.com"
    const val WEB_ITEM_PATH = "$BASE_WEB_URL/item?id=%s"
    const val BASE_API_URL = "https://$HOST/v0/"
    private const val MUTABLE_FIELD_TTL_MILLIS = 30 * 60 * 1000L
  }
}
