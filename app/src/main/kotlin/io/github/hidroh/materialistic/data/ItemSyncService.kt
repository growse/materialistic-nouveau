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

import android.app.Service
import android.content.Intent
import android.os.IBinder

import javax.inject.Inject

import io.github.hidroh.materialistic.ActivityModule
import io.github.hidroh.materialistic.Injectable

open class ItemSyncService : Service() {

    private var sItemSyncAdapter: ItemSyncAdapter? = null
    private const val sItemSyncAdapterLock: Any = new Object()
    @Inject var mFactory: RestServiceFactory? = null
    @Inject var mReadabilityClient: ReadabilityClient? = null

    override fun onCreate() {
        super.onCreate()
        ((Injectable) getApplication())
                .getApplicationGraph()
                .plus(ActivityModule(this))
                .inject(this)
        synchronized (sItemSyncAdapterLock) {
            if (sItemSyncAdapter == null) {
                sItemSyncAdapter = ItemSyncAdapter(getApplicationContext(),
                        mFactory, mReadabilityClient)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return sItemSyncAdapter.getSyncAdapterBinder()
    }
}
