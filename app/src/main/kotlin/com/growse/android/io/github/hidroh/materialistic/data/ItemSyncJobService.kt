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
package com.growse.android.io.github.hidroh.materialistic.data

import android.app.job.JobParameters
import android.app.job.JobService
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ItemSyncJobService : JobService() {
  @Inject lateinit var factory: RestServiceFactory

  @Inject lateinit var readabilityClient: ReadabilityClient

  @Inject lateinit var localCache: LocalCache
  private val mSyncDelegates: MutableMap<String?, SyncDelegate?> = HashMap()

  override fun onStartJob(jobParameters: JobParameters): Boolean {
    val jobId = jobParameters.jobId.toString()
    val syncDelegate = createSyncDelegate()
    mSyncDelegates[jobId] = syncDelegate
    syncDelegate.subscribe { token: String? ->
      if (TextUtils.equals(jobId, token)) {
        jobFinished(jobParameters, false)
        mSyncDelegates.remove(jobId)
      }
    }
    syncDelegate.performSync(SyncDelegate.Job(jobParameters.extras))
    return true
  }

  override fun onStopJob(jobParameters: JobParameters): Boolean {
    val key = jobParameters.jobId.toString()
    if (mSyncDelegates.containsKey(key)) {
      mSyncDelegates.remove(key)!!.stopSync()
    }
    return true
  }

  @VisibleForTesting
  fun createSyncDelegate(): SyncDelegate {
    return SyncDelegate(this, factory, readabilityClient, localCache)
  }
}
