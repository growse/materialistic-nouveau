package com.growse.android.io.github.hidroh.materialistic.testing

import android.util.Log
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

/**
 * A local stand-in for every server the app talks to.
 *
 * [client] sends each request to one in-process TLS [MockWebServer], keeping the real host as the
 * first path segment: `https://hacker-news.firebaseio.com/v0/topstories.json` is served from
 * `/hacker-news.firebaseio.com/v0/topstories.json`. Tests register responses against the real URL
 * with [respond] or [handle]; anything unregistered gets a 404 and is recorded in
 * [unmatchedRequests]. Every request is recorded, with its real URL, in [requests].
 *
 * TLS rather than plain HTTP so the app's cleartext policy doesn't need loosening for tests.
 */
object FakeBackend {
  private const val TAG = "FakeBackend"
  private const val LOCALHOST = "localhost"

  private val serverCertificate =
      HeldCertificate.Builder().addSubjectAlternativeName(LOCALHOST).build()

  private val routes = ConcurrentHashMap<String, (HttpUrl) -> MockResponse>()
  private val recorded = Collections.synchronizedList(mutableListOf<HttpUrl>())
  private val unmatched = Collections.synchronizedList(mutableListOf<String>())

  private val server: MockWebServer by lazy {
    MockWebServer().apply {
      useHttps(
          HandshakeCertificates.Builder()
              .heldCertificate(serverCertificate)
              .build()
              .sslSocketFactory(),
      )
      dispatcher =
          object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
              val key = request.url.encodedPath.removePrefix("/")
              val original = originalUrl(request.url)
              recorded += original
              return routes[key]?.invoke(original)
                  ?: MockResponse(code = 404).also {
                    Log.w(TAG, "No response registered for $key")
                    unmatched += key
                  }
            }
          }
      start(InetAddress.getByName(LOCALHOST), 0)
    }
  }

  /**
   * The app's HTTP client in tests; see
   * [FakeNetworkModule][com.growse.android.io.github.hidroh.materialistic.di.FakeNetworkModule].
   */
  val client: Call.Factory by lazy {
    val trust =
        HandshakeCertificates.Builder().addTrustedCertificate(serverCertificate.certificate).build()
    OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
              val original = chain.request().url
              val target =
                  server
                      .url("/${original.host}${original.encodedPath}")
                      .newBuilder()
                      .encodedQuery(original.encodedQuery)
                      .build()
              chain.proceed(chain.request().newBuilder().url(target).build())
            },
        )
        .sslSocketFactory(trust.sslSocketFactory(), trust.trustManager)
        .followRedirects(false)
        .build()
  }

  /** Every request received so far, by the URL the app asked for. */
  val requests: List<HttpUrl>
    get() = synchronized(recorded) { recorded.toList() }

  /** Paths (host first) that were requested without a registered response. */
  val unmatchedRequests: List<String>
    get() = synchronized(unmatched) { unmatched.toList() }

  /** Serves [body] with [code] for requests to [url], ignoring its query string. */
  fun respond(url: String, body: String = "", code: Int = 200) =
      handle(url) { MockResponse(code = code, body = body) }

  /**
   * Answers requests to [url] with [handler], which gets the URL the app asked for, query string
   * included, so it can vary the response by query.
   */
  fun handle(url: String, handler: (HttpUrl) -> MockResponse) {
    val parsed = url.toHttpUrl()
    routes["${parsed.host}${parsed.encodedPath}"] = handler
  }

  /** Forgets every registered response and recorded request. */
  fun reset() {
    routes.clear()
    recorded.clear()
    unmatched.clear()
  }

  /**
   * Undoes [client]'s rewrite: `/host/path?query` on the fake back to `https://host/path?query`.
   */
  private fun originalUrl(served: HttpUrl): HttpUrl {
    val host = served.pathSegments.first()
    return HttpUrl.Builder()
        .scheme("https")
        .host(host)
        .encodedPath(served.encodedPath.removePrefix("/$host"))
        .encodedQuery(served.encodedQuery)
        .build()
  }
}
