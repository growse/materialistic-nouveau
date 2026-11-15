package com.growse.android.io.github.hidroh.materialistic.testing

import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import org.json.JSONArray
import org.json.JSONObject

/** Serves Hacker News API responses from [FakeBackend], in the shape the Firebase API returns. */
object FakeHackerNews {
  /** An hour ago, so items render with a plausible age. */
  private val postedAt: Long
    get() = System.currentTimeMillis() / 1000 - 3600

  fun topStories(vararg ids: Long) = stories("topstories.json", ids)

  fun newStories(vararg ids: Long) = stories("newstories.json", ids)

  /** Makes the top stories list fail with [code]. */
  fun topStoriesFail(code: Int = 500) = FakeBackend.respond(api("topstories.json"), code = code)

  /** A text story, so opening it never needs a WebView to load an external article. */
  fun story(
      id: Long,
      title: String,
      kids: List<Long> = emptyList(),
      by: String = "author",
      score: Int = 1,
      text: String? = null,
  ) =
      item(
          JSONObject()
              .put("id", id)
              .put("type", "story")
              .put("title", title)
              .put("by", by)
              .put("score", score)
              .put("time", postedAt)
              .put("descendants", kids.size)
              .put("kids", JSONArray(kids))
              .apply { if (text != null) put("text", text) },
      )

  fun comment(
      id: Long,
      parent: Long,
      text: String,
      kids: List<Long> = emptyList(),
      by: String = "commenter",
  ) =
      item(
          JSONObject()
              .put("id", id)
              .put("type", "comment")
              .put("parent", parent)
              .put("text", text)
              .put("by", by)
              .put("time", postedAt)
              .put("kids", JSONArray(kids)),
      )

  private fun stories(path: String, ids: LongArray) =
      FakeBackend.respond(api(path), JSONArray(ids.toList()).toString())

  private fun item(json: JSONObject) =
      FakeBackend.respond(api("item/${json.getLong("id")}.json"), json.toString())

  private fun api(path: String) = HackerNewsClient.BASE_API_URL + path
}
