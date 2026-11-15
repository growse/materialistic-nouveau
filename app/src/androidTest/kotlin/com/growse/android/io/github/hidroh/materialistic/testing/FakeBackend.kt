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
 * with [respond]; anything unregistered gets a 404 and is recorded in [unmatchedRequests].
 *
 * TLS rather than plain HTTP so the app's cleartext policy doesn't need loosening for tests.
 */
object FakeBackend {
  private const val TAG = "FakeBackend"
  private const val LOCALHOST = "localhost"

  private val serverCertificate =
      HeldCertificate.Builder().addSubjectAlternativeName(LOCALHOST).build()

  private val routes = ConcurrentHashMap<String, MockResponse>()
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
              return routes[key]
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

  /** Paths (host first) that were requested without a registered response. */
  val unmatchedRequests: List<String>
    get() = synchronized(unmatched) { unmatched.toList() }

  /** Serves [body] with [code] for requests to [url], ignoring its query string. */
  fun respond(url: String, body: String = "", code: Int = 200) {
    val parsed = url.toHttpUrl()
    routes["${parsed.host}${parsed.encodedPath}"] = MockResponse(code = code, body = body)
  }

  /** Forgets every registered response and recorded miss. */
  fun reset() {
    routes.clear()
    unmatched.clear()
  }
}
