/*
 * Copyright (c) 2015 Ha Duy Trung
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
package com.growse.android.io.github.hidroh.materialistic.data

import android.os.Build
import androidx.annotation.Keep
import com.growse.android.io.github.hidroh.materialistic.BuildConfig
import com.growse.android.io.github.hidroh.materialistic.di.DataModule
import javax.inject.Inject
import javax.inject.Named
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import rx.Observable
import rx.Scheduler

interface FeedbackClient {
  interface Callback {
    fun onSent(success: Boolean)
  }

  fun send(title: String, body: String, callback: Callback)

  class Impl
  @Inject
  constructor(
      factory: RestServiceFactory,
      @param:Named(DataModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler,
  ) : FeedbackClient {
    private val feedbackService =
        factory.rxEnabled(true).create(FeedbackService.GITHUB_API_URL, FeedbackService::class.java)

    override fun send(title: String, body: String, callback: Callback) {
      feedbackService
          .createGithubIssue(
              Issue(
                  title,
                  String.format(
                      "%s\nDevice: %s %s, SDK: %s, app version: %s",
                      body,
                      Build.MANUFACTURER,
                      Build.MODEL,
                      Build.VERSION.SDK_INT,
                      BuildConfig.VERSION_CODE,
                  ),
              )
          )
          .map { true }
          .onErrorReturn { false }
          .observeOn(mainThreadScheduler)
          .subscribe { callback.onSent(it) }
    }

    internal interface FeedbackService {
      @POST("repos/hidroh/materialistic/issues")
      @Headers("Authorization: token " + BuildConfig.GITHUB_TOKEN)
      fun createGithubIssue(@Body issue: Issue): Observable<Any>

      companion object {
        const val GITHUB_API_URL: String = "https://api.github.com/"
      }
    }

    internal class Issue(@field:Keep val title: String?, @field:Keep val body: String?) {
      @field:Keep val labels = arrayOf(LABEL_FEEDBACK)

      companion object {
        private const val LABEL_FEEDBACK = "feedback"
      }
    }
  }
}
