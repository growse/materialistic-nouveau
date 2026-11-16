package com.growse.android.io.github.hidroh.materialistic.data

import android.text.format.DateUtils
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHttp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AlgoliaClientTest :
    StringSpec(
        {
          lateinit var http: FakeHttp
          val sortByTime = AlgoliaClient.sSortByTime

          beforeTest { http = FakeHttp() }

          afterTest { AlgoliaClient.sSortByTime = sortByTime }

          fun hits(vararg ids: Long) =
              ids.joinToString(",", """{"hits":[""", "]}") { """{"objectID":"$it"}""" }

          "searches by date when sorting by most recent" {
            AlgoliaClient.sSortByTime = true
            http.respond("/api/v1/search_by_date", hits(3, 1))

            val stories =
                AlgoliaClient(RestServiceFactory.Impl(http.client))
                    .getStories("kotlin", ItemManager.MODE_DEFAULT)!!

            stories.map { it!!.id } shouldContainExactly listOf("3", "1")
            stories.map { it!!.rank } shouldContainExactly listOf(1, 2)
            http.requests.single().url.queryParameter("query") shouldBe "kotlin"
          }

          "searches by relevance when sorting by popularity" {
            AlgoliaClient.sSortByTime = false
            http.respond("/api/v1/search", hits(7))

            AlgoliaClient(RestServiceFactory.Impl(http.client))
                .getStories("kotlin", ItemManager.MODE_DEFAULT)!!
                .map { it!!.id } shouldContainExactly listOf("7")
            http.requestedPaths.single() shouldBe "/api/v1/search"
          }

          "a search lost to a dropped connection finds nothing" {
            AlgoliaClient.sSortByTime = true
            http.failWithIoException("/api/v1/search_by_date")

            AlgoliaClient(RestServiceFactory.Impl(http.client))
                .getStories("kotlin", ItemManager.MODE_DEFAULT)!!
                .toList()
                .shouldBeEmpty()
          }

          "a search that fails on the server finds nothing" {
            AlgoliaClient.sSortByTime = true
            http.respond("/api/v1/search_by_date", "", code = 500)

            AlgoliaClient(RestServiceFactory.Impl(http.client))
                .getStories("kotlin", ItemManager.MODE_DEFAULT)!!
                .toList()
                .shouldBeEmpty()
          }

          "items are fetched from Hacker News, as Algolia only supplies their ids" {
            val hackerNews = mock<ItemManager>()
            val client =
                AlgoliaClient(RestServiceFactory.Impl(http.client)).apply {
                  mHackerNewsClient = hackerNews
                }

            client.getItem("1", ItemManager.MODE_CACHE)

            verify(hackerNews).getItem("1", ItemManager.MODE_CACHE)
          }

          "each popular range asks for stories created since that long ago" {
            listOf(
                    AlgoliaPopularClient.LAST_24H to DateUtils.DAY_IN_MILLIS,
                    AlgoliaPopularClient.PAST_WEEK to DateUtils.WEEK_IN_MILLIS,
                    AlgoliaPopularClient.PAST_MONTH to DateUtils.WEEK_IN_MILLIS * 4,
                    AlgoliaPopularClient.PAST_YEAR to DateUtils.YEAR_IN_MILLIS,
                )
                .forEach { (range, span) ->
                  http.respond("/api/v1/search", hits(1))
                  val before = (System.currentTimeMillis() - span) / 1000

                  AlgoliaPopularClient(RestServiceFactory.Impl(http.client))
                      .getStories(range, ItemManager.MODE_DEFAULT)

                  val request = http.requests.last()
                  request.url.encodedPath shouldBe "/api/v1/search"
                  val filter = request.url.queryParameter("numericFilters")!!
                  filter.substringBefore('>') shouldBe "created_at_i"
                  filter.substringAfter('>').toLong().shouldBeBetween(before, before + 5)
                }
          }

          "popular stories are ranked by relevance whatever the search sort" {
            AlgoliaClient.sSortByTime = true
            http.respond("/api/v1/search", hits(9, 8))

            AlgoliaPopularClient(RestServiceFactory.Impl(http.client))
                .getStories(AlgoliaPopularClient.PAST_WEEK, ItemManager.MODE_DEFAULT)!!
                .map { it!!.id } shouldContainExactly listOf("9", "8")
          }
        },
    )
