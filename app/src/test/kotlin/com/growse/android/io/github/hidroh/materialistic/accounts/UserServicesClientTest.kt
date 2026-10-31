package com.growse.android.io.github.hidroh.materialistic.accounts

import android.content.Context
import com.growse.android.io.github.hidroh.materialistic.R
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import java.io.IOException
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.mockito.kotlin.mock
import rx.Scheduler
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class UserServicesClientTest :
    StringSpec({
      beforeSpec {
        // deliver callbacks synchronously instead of posting to a main looper the JVM doesn't have
        RxAndroidPlugins.getInstance().reset()
        RxAndroidPlugins.getInstance()
            .registerSchedulersHook(
                object : RxAndroidSchedulersHook() {
                  override fun getMainThreadScheduler(): Scheduler = Schedulers.immediate()
                }
            )
        AndroidSchedulers.reset()
      }

      afterSpec {
        RxAndroidPlugins.getInstance().reset()
        AndroidSchedulers.reset()
      }

      val context = mock<Context>()
      val credentials = "alice" to "hunter2"

      "login posts the credentials to the login page" {
        val server = FakeHackerNews(redirect("news"))

        server.client().login("alice", "hunter2", false, RecordingCallback())

        server.requests shouldHaveSize 1
        server.requests[0].url.toString() shouldBe "https://news.ycombinator.com/login"
        server.requests[0].form() shouldContainExactly
            mapOf("acct" to "alice", "pw" to "hunter2", "goto" to "news")
      }

      "login flags account creation" {
        val server = FakeHackerNews(redirect("news"))

        server.client().login("alice", "hunter2", true, RecordingCallback())

        server.requests[0].form()["creating"] shouldBe "t"
      }

      "login succeeds on a redirect" {
        val server = FakeHackerNews(redirect("news"))
        val callback = RecordingCallback()

        server.client().login("alice", "hunter2", false, callback)

        callback.done shouldBe true
        callback.error.shouldBeNull()
        server.allClosed() shouldBe true
      }

      "login reports the error message from a re-rendered login page" {
        val server = FakeHackerNews(ok("<html><body>Bad login.\n  </body></html>"))
        val callback = RecordingCallback()

        server.client().login("alice", "wrong", false, callback)

        callback.done.shouldBeNull()
        callback.error.shouldBeTypeOf<UserServices.Exception>().message shouldBe "Bad login."
        server.allClosed() shouldBe true
      }

      "login fails without an error on any other response" {
        val server = FakeHackerNews(response(500))
        val callback = RecordingCallback()

        server.client().login("alice", "hunter2", false, callback)

        callback.done shouldBe false
        callback.error.shouldBeNull()
      }

      "login reports network errors" {
        val server = FakeHackerNews({ throw IOException("offline") })
        val callback = RecordingCallback()

        server.client().login("alice", "hunter2", false, callback)

        callback.done.shouldBeNull()
        callback.error.shouldBeInstanceOf<IOException>().message shouldBe "offline"
      }

      "voting without credentials does nothing" {
        val server = FakeHackerNews()
        val callback = RecordingCallback()

        server.client(credentials = null).voteUp(context, "123", callback) shouldBe false

        server.requests.shouldBeEmpty()
        callback.done.shouldBeNull()
      }

      "replying without credentials fails without sending anything" {
        val server = FakeHackerNews()
        val callback = RecordingCallback()

        server.client(credentials = null).reply(context, "123", "hi", callback)

        server.requests.shouldBeEmpty()
        callback.done shouldBe false
      }

      "replying posts the comment and succeeds on a redirect" {
        val server = FakeHackerNews(redirect("item?id=123"))
        val callback = RecordingCallback()

        server.client(credentials).reply(context, "123", "Nice post", callback)

        server.requests[0].url.toString() shouldBe "https://news.ycombinator.com/comment"
        server.requests[0].form() shouldContainExactly
            mapOf("acct" to "alice", "pw" to "hunter2", "parent" to "123", "text" to "Nice post")
        callback.done shouldBe true
        server.allClosed() shouldBe true
      }

      "replying fails when not redirected" {
        val server = FakeHackerNews(ok("<html>try again</html>"))
        val callback = RecordingCallback()

        server.client(credentials).reply(context, "123", "Nice post", callback)

        callback.done shouldBe false
      }

      "submitting without credentials fails without sending anything" {
        val server = FakeHackerNews()
        val callback = RecordingCallback()

        server.client(credentials = null).submit(context, "Title", "https://a.b", true, callback)

        server.requests.shouldBeEmpty()
        callback.done shouldBe false
      }

      "submitting a link posts the form token, cookie and url" {
        val server = FakeHackerNews(submitPage(fnid = "abc123"), redirect("newest"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        server.requests shouldHaveSize 2
        server.requests[0].url.toString() shouldBe "https://news.ycombinator.com/submit"
        server.requests[0].form() shouldContainExactly mapOf("acct" to "alice", "pw" to "hunter2")
        server.requests[1].url.toString() shouldBe "https://news.ycombinator.com/r"
        server.requests[1].header("cookie") shouldBe "user=alice&token"
        server.requests[1].form() shouldContainExactly
            mapOf(
                "fnid" to "abc123",
                "fnop" to "submit-page",
                "title" to "Title",
                "url" to "https://a.b",
            )
        callback.done shouldBe true
        callback.error.shouldBeNull()
        server.allClosed() shouldBe true
      }

      "submitting text sends it as text rather than url" {
        val server = FakeHackerNews(submitPage(fnid = "abc123"), redirect("newest"))

        server
            .client(credentials)
            .submit(context, "Ask HN", "Question?", false, RecordingCallback())

        server.requests[1].form()["text"] shouldBe "Question?"
        server.requests[1].form()["url"].shouldBeNull()
      }

      "submitting accepts an absolute redirect to newest" {
        val server =
            FakeHackerNews(
                submitPage(fnid = "abc123"),
                redirect("https://news.ycombinator.com/newest"),
            )
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        callback.done shouldBe true
      }

      "submitting fails when the submit page redirects to login" {
        val server = FakeHackerNews(redirect("login"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        server.requests shouldHaveSize 1
        callback.error.shouldBeInstanceOf<IOException>()
        server.allClosed() shouldBe true
      }

      "submitting fails when the submit page has no form token" {
        val server = FakeHackerNews(ok("<html><body>no form here</body></html>"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        server.requests shouldHaveSize 1
        callback.error.shouldBeInstanceOf<IOException>()
      }

      "submitting a duplicate reports the existing item" {
        val server = FakeHackerNews(submitPage(fnid = "abc123"), redirect("item?id=42"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        val error = callback.error.shouldBeTypeOf<UserServices.Exception>()
        error.messageResId shouldBe R.string.item_exist
        error.itemId shouldBe "42"
      }

      "submitting fails on any other redirect" {
        val server = FakeHackerNews(submitPage(fnid = "abc123"), redirect("x?fnid=abc123"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        callback.error.shouldBeTypeOf<IOException>()
      }

      "submitting fails when the post is not redirected" {
        val server = FakeHackerNews(submitPage(fnid = "abc123"), ok("<html>error</html>"))
        val callback = RecordingCallback()

        server.client(credentials).submit(context, "Title", "https://a.b", true, callback)

        callback.error.shouldBeTypeOf<IOException>()
        server.allClosed() shouldBe true
      }
    })

private class RecordingCallback : UserServices.Callback() {
  var done: Boolean? = null
  var error: Throwable? = null

  override fun onDone(successful: Boolean) {
    done = successful
  }

  override fun onError(throwable: Throwable?) {
    error = throwable
  }
}

private typealias Reply = (Request) -> Response

/** Answers each request with the next queued [Reply], recording requests and response bodies. */
private class FakeHackerNews(vararg replies: Reply) {
  private val replies = ArrayDeque(replies.toList())
  private val bodies = mutableListOf<TrackingBody>()
  val requests = mutableListOf<Request>()

  fun client(credentials: Pair<String, String>? = null): UserServicesClient {
    val callFactory =
        OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor { chain ->
              val request = chain.request()
              requests += request
              replies.removeFirst()(request).also { response ->
                (response.body as? TrackingBody)?.let(bodies::add)
              }
            }
            .build()
    return UserServicesClient(callFactory, Schedulers.immediate()) { credentials }
  }

  fun allClosed() = bodies.all { it.closed }
}

private class TrackingBody(content: String) : ResponseBody() {
  private val buffer = Buffer().writeUtf8(content)
  var closed = false

  override fun contentType() = "text/html".toMediaType()

  override fun contentLength() = buffer.size

  override fun source(): BufferedSource = buffer

  override fun close() {
    closed = true
    super.close()
  }
}

private fun response(
    code: Int,
    body: String = "",
    headers: Map<String, String> = emptyMap(),
): Reply = { request ->
  Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message("HTTP $code")
      .apply { headers.forEach { (name, value) -> header(name, value) } }
      .body(TrackingBody(body))
      .build()
}

private fun ok(body: String) = response(200, body)

private fun redirect(location: String) = response(302, headers = mapOf("location" to location))

private fun submitPage(fnid: String) =
    response(
        200,
        body =
            """<form method="post" action="r">""" +
                """<input type="hidden" name="fnid" value="$fnid">""" +
                """<input type="hidden" name="fnop" value="submit-page"></form>""",
        headers = mapOf("set-cookie" to "user=alice&token"),
    )

private fun Request.form(): Map<String, String> {
  val form = body as FormBody
  return (0 until form.size).associate { form.name(it) to form.value(it) }
}
