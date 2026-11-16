package com.growse.android.io.github.hidroh.materialistic.data

import com.growse.android.io.github.hidroh.materialistic.testing.FakeHttp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import rx.Observable
import rx.schedulers.Schedulers

class HackerNewsClientTest :
    StringSpec(
        {
          lateinit var http: FakeHttp
          lateinit var cache: LocalCache
          lateinit var favoriteManager: FavoriteManager
          lateinit var client: HackerNewsClient

          beforeTest {
            http = FakeHttp()
            cache = mock()
            favoriteManager = mock()
            whenever(favoriteManager.check(any())).thenReturn(Observable.just(false))
            client =
                HackerNewsClient(
                    RestServiceFactory.Impl(http.client),
                    SessionManager(Schedulers.immediate(), cache),
                    favoriteManager,
                    cache,
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                )
          }

          fun cached(id: String, ageMillis: Long, title: String = "Cached") =
              MaterialisticDatabase.CachedItem(id).apply {
                type = "story"
                this.title = title
                fetchedAt = System.currentTimeMillis() - ageMillis
              }

          "the web item path is built from the base web url" {
            String.format(HackerNewsClient.WEB_ITEM_PATH, "1234") shouldBe
                "https://news.ycombinator.com/item?id=1234"
          }

          "the api host matches the base web url's provider" {
            HackerNewsClient.HOST shouldBe "hacker-news.firebaseio.com"
            HackerNewsClient.BASE_WEB_URL shouldBe "https://news.ycombinator.com"
          }

          "each story list is read from its own feed" {
            listOf(
                    ItemManager.TOP_FETCH_MODE to "topstories.json",
                    ItemManager.NEW_FETCH_MODE to "newstories.json",
                    ItemManager.BEST_FETCH_MODE to "beststories.json",
                    ItemManager.SHOW_FETCH_MODE to "showstories.json",
                    ItemManager.ASK_FETCH_MODE to "askstories.json",
                    ItemManager.JOBS_FETCH_MODE to "jobstories.json",
                )
                .forEach { (filter, feed) ->
                  http.respond("/v0/$feed", "[1]")

                  client.getStories(filter, ItemManager.MODE_DEFAULT)!!.map { it!!.id } shouldBe
                      listOf("1")
                  http.requestedPaths.last() shouldBe "/v0/$feed"
                }
          }

          "stories are ranked in the order the feed lists them" {
            http.respond("/v0/topstories.json", "[30, 10, 20]")

            val stories = client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT)!!

            stories.map { it!!.id } shouldContainExactly listOf("30", "10", "20")
            stories.map { it!!.rank } shouldContainExactly listOf(1, 2, 3)
          }

          "a refresh asks for the feed from the network rather than the HTTP cache" {
            http.respond("/v0/topstories.json", "[1]")

            client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_NETWORK)
            client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT)

            http.requests.map { it.header("Cache-Control") } shouldContainExactly
                listOf("no-cache", "max-age=1800")
          }

          "a feed that fails on the server gives no list, so the caller can show an error" {
            http.respond("/v0/topstories.json", "", code = 500)

            client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT).shouldBeNull()
          }

          "a feed lost to a dropped connection gives an empty list" {
            http.failWithIoException("/v0/topstories.json")

            client
                .getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT)!!
                .toList()
                .shouldBeEmpty()
          }

          "a freshly cached item is served from the cache without a request" {
            whenever(cache.getCachedItem("1")).thenReturn(cached("1", ageMillis = 60_000))

            client.getItem("1", ItemManager.MODE_DEFAULT)!!.title shouldBe "Cached"
            http.requests.shouldBeEmpty()
          }

          "a stale cached item is fetched again, and the fresh copy cached" {
            whenever(cache.getCachedItem("1")).thenReturn(cached("1", ageMillis = 31 * 60_000))
            http.respond("/v0/item/1.json", """{"id":1,"type":"story","title":"Fresh"}""")

            client.getItem("1", ItemManager.MODE_DEFAULT)!!.title shouldBe "Fresh"
            val stored = argumentCaptor<MaterialisticDatabase.CachedItem>()
            verify(cache).putCachedItem(stored.capture())
            stored.firstValue.title shouldBe "Fresh"
          }

          "cache mode serves even a stale cached item without a request" {
            whenever(cache.getCachedItem("1")).thenReturn(cached("1", ageMillis = 31 * 60_000))

            client.getItem("1", ItemManager.MODE_CACHE)!!.title shouldBe "Cached"
            http.requests.shouldBeEmpty()
          }

          "network mode fetches even a freshly cached item" {
            whenever(cache.getCachedItem("1")).thenReturn(cached("1", ageMillis = 60_000))
            http.respond("/v0/item/1.json", """{"id":1,"type":"story","title":"Fresh"}""")

            client.getItem("1", ItemManager.MODE_NETWORK)!!.title shouldBe "Fresh"
            verify(cache, never()).getCachedItem(any())
            http.requests.single().header("Cache-Control") shouldBe "no-cache"
          }

          "an item lost to a dropped connection gives nothing, and nothing is cached" {
            http.failWithIoException("/v0/item/1.json")

            client.getItem("1", ItemManager.MODE_DEFAULT).shouldBeNull()
            verify(cache, never()).putCachedItem(any())
          }

          "a fetched item carries whether it's been viewed and saved" {
            http.respond("/v0/item/1.json", """{"id":1,"type":"story","title":"Story"}""")
            whenever(cache.isViewed("1")).thenReturn(true)
            whenever(favoriteManager.check("1")).thenReturn(Observable.just(true))

            val item = awaitResponse<Item?> { client.getItem("1", ItemManager.MODE_DEFAULT, it) }

            item.shouldNotBeNull()
            item.isViewed shouldBe true
            item.isFavorite shouldBe true
          }

          "a user's submissions are listed as items to load, in the order given" {
            http.respond(
                "/v0/user/alice.json",
                """{"id":"alice","karma":42,"created":1,"submitted":[5002,5001]}""",
            )

            val user = awaitResponse<UserManager.User?> { client.getUser("alice", it) }

            user.shouldNotBeNull()
            user.karma shouldBe 42
            user.items.map { it!!.id } shouldContainExactly listOf("5002", "5001")
          }
        },
    ) {
  private companion object {
    /** Runs [request] and waits for the response it delivers to its listener. */
    fun <T> awaitResponse(request: (ResponseListener<T>) -> Unit): T? {
      val result = CompletableFuture<T?>()
      request(
          object : ResponseListener<T> {
            override fun onResponse(response: T?) {
              result.complete(response)
            }

            override fun onError(errorMessage: String?) {
              result.completeExceptionally(AssertionError("request failed: $errorMessage"))
            }
          },
      )
      return result.get(5, TimeUnit.SECONDS)
    }
  }
}
