package com.growse.android.io.github.hidroh.materialistic.testing

import mockwebserver3.MockResponse
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serves HN Algolia search responses from [FakeBackend]. The app only asks Algolia for story ids
 * and fetches each story from the HN API, so serve those with [FakeHackerNews.story].
 */
object FakeAlgolia {
  private const val BASE = "https://hn.algolia.com/api/v1/"

  /** Search sorted by relevance, which also backs the popular ("Catch Up") lists. */
  const val BY_RELEVANCE = "search"

  /** Search sorted by date, the app's default sort. */
  const val BY_DATE = "search_by_date"

  /** Answers [endpoint] with the story ids [hitsFor] picks for each request, in rank order. */
  fun search(endpoint: String, hitsFor: (HttpUrl) -> List<Long>) =
      FakeBackend.handle(BASE + endpoint) { url -> MockResponse(body = hits(hitsFor(url))) }

  /** Answers [endpoint] with [ids], whatever was asked. */
  fun search(endpoint: String, vararg ids: Long) = search(endpoint) { ids.toList() }

  /**
   * The age, in seconds, of the oldest story the popular list asked for: it filters on
   * `numericFilters=created_at_i>{timestamp}`.
   */
  fun requestedMaxAgeSeconds(url: HttpUrl): Long? =
      url.queryParameter("numericFilters")?.substringAfter('>')?.toLongOrNull()?.let {
        System.currentTimeMillis() / 1000 - it
      }

  private fun hits(ids: List<Long>) =
      JSONObject()
          .put("hits", JSONArray(ids.map { JSONObject().put("objectID", it.toString()) }))
          .toString()
}
