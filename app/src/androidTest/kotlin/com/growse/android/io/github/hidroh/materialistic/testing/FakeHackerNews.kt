package com.growse.android.io.github.hidroh.materialistic.testing

import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import org.json.JSONArray
import org.json.JSONObject

/** Serves Hacker News API responses from [FakeBackend], in the shape the Firebase API returns. */
object FakeHackerNews {
  /** The story lists the API publishes, by the file each is served from. */
  enum class Feed(val path: String) {
    Top("topstories.json"),
    New("newstories.json"),
    Best("beststories.json"),
    Show("showstories.json"),
    Ask("askstories.json"),
    Jobs("jobstories.json"),
  }

  /** An hour ago, so items render with a plausible age. */
  private val postedAt: Long
    get() = System.currentTimeMillis() / 1000 - 3600

  /** Serves [ids], in rank order, as [feed]. */
  fun feed(feed: Feed, vararg ids: Long) =
      FakeBackend.respond(api(feed.path), JSONArray(ids.toList()).toString())

  /** Makes [feed] fail with [code]. */
  fun feedFails(feed: Feed, code: Int = 500) = FakeBackend.respond(api(feed.path), code = code)

  fun topStories(vararg ids: Long) = feed(Feed.Top, *ids)

  /** Makes the top stories list fail with [code]. */
  fun topStoriesFail(code: Int = 500) = feedFails(Feed.Top, code)

  /**
   * A story. With no [url] it's a text story, so opening it never needs a WebView to load an
   * external article.
   */
  fun story(
      id: Long,
      title: String,
      kids: List<Long> = emptyList(),
      by: String = "author",
      score: Int = 1,
      text: String? = null,
      url: String? = null,
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
              .apply {
                if (text != null) put("text", text)
                if (url != null) put("url", url)
              },
      )

  /** A comment; [dead] ones are those flagged and hidden from logged-out readers on HN. */
  fun comment(
      id: Long,
      parent: Long,
      text: String,
      kids: List<Long> = emptyList(),
      by: String = "commenter",
      dead: Boolean = false,
  ) =
      item(
          JSONObject()
              .put("id", id)
              .put("type", "comment")
              .put("parent", parent)
              .put("text", text)
              .put("by", by)
              .put("time", postedAt)
              .put("kids", JSONArray(kids))
              .apply { if (dead) put("dead", true) },
      )

  /** A deleted comment, which HN keeps with its replies but strips of its author and text. */
  fun deletedComment(id: Long, parent: Long, kids: List<Long> = emptyList()) =
      item(
          JSONObject()
              .put("id", id)
              .put("type", "comment")
              .put("parent", parent)
              .put("deleted", true)
              .put("time", postedAt)
              .put("kids", JSONArray(kids)),
      )

  /** A user profile; [submitted] lists their stories and comments, newest first. */
  fun user(
      id: String,
      karma: Long = 1,
      about: String? = null,
      submitted: List<Long> = emptyList(),
  ) =
      FakeBackend.respond(
          api("user/$id.json"),
          JSONObject()
              .put("id", id)
              .put("karma", karma)
              .put("created", postedAt)
              .put("submitted", JSONArray(submitted))
              .apply { if (about != null) put("about", about) }
              .toString(),
      )

  /** What the API returns for a username that doesn't exist: a literal `null`. */
  fun noSuchUser(id: String) = FakeBackend.respond(api("user/$id.json"), "null")

  private fun item(json: JSONObject) =
      FakeBackend.respond(api("item/${json.getLong("id")}.json"), json.toString())

  private fun api(path: String) = HackerNewsClient.BASE_API_URL + path
}
