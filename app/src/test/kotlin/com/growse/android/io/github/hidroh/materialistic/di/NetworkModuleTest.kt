// NetworkInfo is deprecated, but it is what AppUtils.hasConnection reads.
@file:Suppress("DEPRECATION")

package com.growse.android.io.github.hidroh.materialistic.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.ReadabilityClient
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NetworkModuleTest :
    StringSpec(
        {
          /** A context whose connectivity reports [online]. */
          fun context(online: Boolean): Context {
            val networkInfo = mock<NetworkInfo> { on { isConnectedOrConnecting } doReturn online }
            val connectivity =
                mock<ConnectivityManager> { on { activeNetworkInfo } doReturn networkInfo }
            return mock<Context> {
              on { applicationContext } doReturn it
              on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivity
            }
          }

          /**
           * Sends [url] through [interceptor] to a server that answers with [serverCacheControl].
           */
          fun send(
              interceptor: Interceptor,
              url: String,
              serverCacheControl: String = "no-store",
          ): Pair<Request, Response> {
            var sent: Request? = null
            val response =
                OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(
                        Interceptor { chain ->
                          sent = chain.request()
                          Response.Builder()
                              .request(chain.request())
                              .protocol(Protocol.HTTP_1_1)
                              .code(200)
                              .message("OK")
                              .header("Cache-Control", serverCacheControl)
                              .body("{}".toResponseBody("application/json".toMediaType()))
                              .build()
                        },
                    )
                    .build()
                    .newCall(Request.Builder().url(url).build())
                    .execute()
            return sent!! to response
          }

          val hackerNews = "https://${HackerNewsClient.HOST}/v0/topstories.json"
          val algolia = "https://${AlgoliaClient.HOST}/api/v1/search"
          val readability = "https://${ReadabilityClient.HOST}/parser"
          val elsewhere = "https://example.com/article"

          "responses from the HN API are cacheable for 30 minutes whatever the server says" {
            val (_, response) = send(NetworkModule.CacheOverrideNetworkInterceptor(), hackerNews)

            response.cacheControl.maxAgeSeconds shouldBe 30 * 60
          }

          "responses from Algolia are cacheable for 30 minutes" {
            val (_, response) = send(NetworkModule.CacheOverrideNetworkInterceptor(), algolia)

            response.cacheControl.maxAgeSeconds shouldBe 30 * 60
          }

          "readability responses are cacheable for a day" {
            val (_, response) = send(NetworkModule.CacheOverrideNetworkInterceptor(), readability)

            response.cacheControl.maxAgeSeconds shouldBe 24 * 60 * 60
          }

          "other hosts keep the caching their server asked for" {
            val (_, response) =
                send(NetworkModule.CacheOverrideNetworkInterceptor(), elsewhere, "max-age=5")

            response.cacheControl.maxAgeSeconds shouldBe 5
          }

          "offline, requests to cached hosts are served from the cache only" {
            val interceptor = NetworkModule.ConnectionAwareInterceptor(context(online = false))

            send(interceptor, hackerNews).first.cacheControl.onlyIfCached shouldBe true
          }

          "online, requests to cached hosts go to the network as usual" {
            val interceptor = NetworkModule.ConnectionAwareInterceptor(context(online = true))

            send(interceptor, hackerNews).first.cacheControl.onlyIfCached shouldBe false
          }

          "offline, requests to other hosts aren't forced to the cache" {
            val interceptor = NetworkModule.ConnectionAwareInterceptor(context(online = false))

            send(interceptor, elsewhere).first.cacheControl.onlyIfCached shouldBe false
          }
        },
    )
