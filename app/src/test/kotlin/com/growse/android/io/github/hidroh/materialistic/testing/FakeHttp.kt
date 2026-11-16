package com.growse.android.io.github.hidroh.materialistic.testing

import java.io.IOException
import java.util.Collections
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * An [OkHttpClient] that answers from canned responses instead of the network, for JVM tests of
 * code that builds its own Retrofit services. Responses are keyed by URL path; anything else gets a
 * 404. Every request is kept in [requests].
 */
class FakeHttp {
  private val responses = mutableMapOf<String, (Request) -> Response>()

  /** Every request made, in order. */
  val requests: MutableList<Request> = Collections.synchronizedList(mutableListOf())

  val client: OkHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(
              Interceptor { chain ->
                val request = chain.request()
                requests += request
                (responses[request.url.encodedPath] ?: ::notFound)(request)
              },
          )
          .build()

  /** Serves [body] with [code] for requests to [path]. */
  fun respond(path: String, body: String, code: Int = 200) {
    responses[path] = { request -> response(request, code, body) }
  }

  /** Fails requests to [path] as a dropped connection would. */
  fun failWithIoException(path: String) {
    responses[path] = { throw IOException("connection dropped") }
  }

  /** The paths requested, in order. */
  val requestedPaths: List<String>
    get() = synchronized(requests) { requests.map { it.url.encodedPath } }

  private fun notFound(request: Request) = response(request, 404, "")

  private fun response(request: Request, code: Int, body: String) =
      Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_1)
          .code(code)
          .message("fake")
          .body(body.toResponseBody(JSON))
          .build()

  private companion object {
    val JSON = "application/json".toMediaType()
  }
}
