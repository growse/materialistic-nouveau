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

package io.github.hidroh.materialistic.data

import android.os.Build
import androidx.annotation.Keep

import javax.inject.Inject
import javax.inject.Named

import io.github.hidroh.materialistic.BuildConfig
import io.github.hidroh.materialistic.DataModule
import io.github.hidroh.materialistic.annotation.Synthetic
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

    open class Impl : FeedbackClient {
        private var mFeedbackService: FeedbackService? = null
        private var mMainThreadScheduler: Scheduler? = null

        @Inject
        constructor(factory: RestServiceFactory, mainThreadScheduler: Scheduler) {
            mFeedbackService = factory.rxEnabled(true)
                    .create(FeedbackService.GITHUB_API_URL, FeedbackService::class.java)
            mMainThreadScheduler = mainThreadScheduler
        }

        override fun send(title: String, body: String, callback: Callback) {
            body = String.format("%s\nDevice: %s %s, SDK: %s, app version: %s",
                    body,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.SDK_INT,
                    BuildConfig.VERSION_CODE)
            mFeedbackService.createGithubIssue(Issue(title, body))
                    .map(response -> true)
                    .onErrorReturn(throwable -> false)
                    .observeOn(mMainThreadScheduler)
                    .subscribe(callback::onSent)
        }

        interface FeedbackService {
            var GITHUB_API_URL: String = "https://api.github.com/"

            @POST("repos/hidroh/materialistic/issues")
            @Headers("Authorization: token " + BuildConfig.GITHUB_TOKEN)
            fun createGithubIssue(issue: Issue): Observable<Any>
        }

        open class Issue {
            private const val LABEL_FEEDBACK: String = "feedback"

            @Keep @Synthetic
            var title: String? = null
            @Keep @Synthetic
            var body: String? = null
            @Keep @Synthetic
            var labels: Array<String>? = null

            @Synthetic
            constructor(title: String, body: String) {
                this.title = title
                this.body = body
                this.labels = String[]{LABEL_FEEDBACK}
            }
        }
    }
}
