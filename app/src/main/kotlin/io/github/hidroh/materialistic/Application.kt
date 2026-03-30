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

package io.github.hidroh.materialistic

import android.content.Context
import android.graphics.Typeface
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate

import dagger.ObjectGraph
import io.github.hidroh.materialistic.data.AlgoliaClient
import rx.schedulers.Schedulers

open class Application : android.app.Application(), Injectable {

    var TYPE_FACE: Typeface? = null
    private var mApplicationGraph: ObjectGraph? = null

    protected override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        mApplicationGraph = ObjectGraph.create()
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Preferences.Theme.getAutoDayNightMode(this))
        AlgoliaClient.sSortByTime = Preferences.isSortByRecent(this)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }
        Preferences.migrate(this)
        TYPE_FACE = FontCache.getInstance().get(this, Preferences.Theme.getTypeface(this))
        AppUtils.registerAccountsUpdatedListener(this)
        AdBlocker.init(this, Schedulers.io())
    }

    override fun inject(`object`: Any) {
        getApplicationGraph().inject(object)
    }

    override fun getApplicationGraph(): ObjectGraph {
        return mApplicationGraph
    }
}
