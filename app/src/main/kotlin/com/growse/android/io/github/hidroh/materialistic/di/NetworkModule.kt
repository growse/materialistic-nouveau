/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growse.android.io.github.hidroh.materialistic.di

import android.content.Context
import android.net.TrafficStats
import android.util.Log
import com.growse.android.io.github.hidroh.materialistic.AppUtils
import com.growse.android.io.github.hidroh.materialistic.BuildConfig
import com.growse.android.io.github.hidroh.materialistic.data.AlgoliaClient
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.ReadabilityClient
import com.growse.android.io.github.hidroh.materialistic.data.RestServiceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.inject.Singleton
import javax.net.SocketFactory
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
internal class NetworkModule {
  @Provides
  @Singleton
  fun provideRestServiceFactory(callFactory: Call.Factory): RestServiceFactory {
    return RestServiceFactory.Impl(callFactory)
  }

  @Provides
  @Singleton
  fun provideCallFactory(@ApplicationContext context: Context): Call.Factory {
    return OkHttpClient.Builder()
        .socketFactory(
            object : SocketFactory() {
              private val defaultFactory: SocketFactory = getDefault()

              @Throws(IOException::class)
              override fun createSocket(): Socket? {
                val socket = defaultFactory.createSocket()
                TrafficStats.setThreadStatsTag(1)
                return socket
              }

              @Throws(IOException::class)
              override fun createSocket(host: String?, port: Int): Socket? {
                val socket = defaultFactory.createSocket(host, port)
                TrafficStats.setThreadStatsTag(1)
                return socket
              }

              @Throws(IOException::class)
              override fun createSocket(
                  host: String?,
                  port: Int,
                  localHost: InetAddress?,
                  localPort: Int,
              ): Socket? {
                val socket = defaultFactory.createSocket(host, port, localHost, localPort)
                TrafficStats.setThreadStatsTag(1)
                return socket
              }

              @Throws(IOException::class)
              override fun createSocket(host: InetAddress?, port: Int): Socket? {
                val socket = defaultFactory.createSocket(host, port)
                TrafficStats.setThreadStatsTag(1)
                return socket
              }

              @Throws(IOException::class)
              override fun createSocket(
                  address: InetAddress?,
                  port: Int,
                  localAddress: InetAddress?,
                  localPort: Int,
              ): Socket? {
                val socket = defaultFactory.createSocket(address, port, localAddress, localPort)
                TrafficStats.setThreadStatsTag(1)
                return socket
              }
            }
        )
        .cache(Cache(context.applicationContext.cacheDir, CACHE_SIZE))
        .addNetworkInterceptor(CacheOverrideNetworkInterceptor())
        .addInterceptor(ConnectionAwareInterceptor(context))
        .addInterceptor(LoggingInterceptor())
        .followRedirects(false)
        .build()
  }

  internal class ConnectionAwareInterceptor(context: Context) : Interceptor {
    private val context: Context = context.applicationContext

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val forceCache =
          CACHE_ENABLED_HOSTS.containsKey(request.url.host) && !AppUtils.hasConnection(context)
      return chain.proceed(
          if (forceCache) request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
          else request
      )
    }

    companion object {
      val CACHE_ENABLED_HOSTS: MutableMap<String?, String?> = HashMap()

      init {
        CACHE_ENABLED_HOSTS[HackerNewsClient.HOST] = RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M
        CACHE_ENABLED_HOSTS[AlgoliaClient.HOST] = RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M
        CACHE_ENABLED_HOSTS[ReadabilityClient.HOST] = RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H
      }
    }
  }

  internal class CacheOverrideNetworkInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val response = chain.proceed(request)
      return if (!ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS.containsKey(request.url.host)) {
        response
      } else {
        response
            .newBuilder()
            .header(
                "Cache-Control",
                ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS[request.url.host]!!,
            )
            .build()
      }
    }
  }

  internal class LoggingInterceptor : Interceptor {
    private val debugInterceptor: Interceptor = HttpLoggingInterceptor { message: String? ->
      Log.d(
          TAG_OK_HTTP,
          message!!,
      )
    }
        .setLevel(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        )

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
      return debugInterceptor.intercept(chain)
    }
  }

  companion object {
    private const val TAG_OK_HTTP = "OkHttp"
    private const val CACHE_SIZE =
        (20 * 1024 * 1024 // 20 MB
            )
            .toLong()
  }
}
