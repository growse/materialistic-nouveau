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

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting

/**
 * Simple sync adapter that triggers OkHttp requests so their responses become available in
 * cache for subsequent requests
 */
open class ItemSyncAdapter : AbstractThreadedSyncAdapter() {
    private var mFactory: RestServiceFactory? = null
    private var mReadabilityClient: ReadabilityClient? = null

    constructor(context: Context, factory: RestServiceFactory, readabilityClient: ReadabilityClient) : super(context, true) {
        mFactory = factory
        mReadabilityClient = readabilityClient
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        createSyncDelegate().performSync(SyncDelegate.Job(extras))
    }

    @VisibleForTesting
    @NonNull
    fun createSyncDelegate(): SyncDelegate {
        return SyncDelegate(getContext(), mFactory, mReadabilityClient)
    }
}
