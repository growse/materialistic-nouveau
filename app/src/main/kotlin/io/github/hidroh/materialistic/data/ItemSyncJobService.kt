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

package io.github.hidroh.materialistic.data

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import android.text.TextUtils

import java.util.HashMap
import java.util.Map

import javax.inject.Inject

import io.github.hidroh.materialistic.ActivityModule
import io.github.hidroh.materialistic.Injectable

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class ItemSyncJobService : JobService() {
    @Inject var mFactory: RestServiceFactory? = null
    @Inject var mReadabilityClient: ReadabilityClient? = null
    private val mSyncDelegates: Map<String, SyncDelegate> = new HashMap<>()

    override fun onCreate() {
        super.onCreate()
        ((Injectable) getApplication())
                .getApplicationGraph()
                .plus(ActivityModule(this))
                .inject(this)
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        val jobId = String.valueOf(jobParameters.getJobId())
        val syncDelegate = createSyncDelegate()
        mSyncDelegates.put(jobId, syncDelegate)
        syncDelegate.subscribe(token -> {
            if (TextUtils.equals(jobId, token)) {
                jobFinished(jobParameters, false)
                mSyncDelegates.remove(jobId)
            }
        })
        syncDelegate.performSync(SyncDelegate.Job(jobParameters.getExtras()))
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        val key = String.valueOf(jobParameters.getJobId())
        if (mSyncDelegates.containsKey(key)) {
            mSyncDelegates.remove(key).stopSync()
        }
        return true
    }

    @VisibleForTesting
    @NonNull
    fun createSyncDelegate(): SyncDelegate {
        return SyncDelegate(this, mFactory, mReadabilityClient)
    }
}
