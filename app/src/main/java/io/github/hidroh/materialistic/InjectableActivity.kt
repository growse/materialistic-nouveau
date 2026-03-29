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

import android.os.Bundle

import dagger.ObjectGraph

abstract class InjectableActivity : ThemedActivity(), Injectable {
    private var mActivityGraph: ObjectGraph? = null
    private var mDestroyed: Boolean = false

    protected override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        inject(this)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        mDestroyed = true
        mActivityGraph = null
    }

    override fun onBackPressed() {
        // TODO http://b.android.com/176265
        try {
            super.onBackPressed()
        } catch (IllegalStateException e) {
            supportFinishAfterTransition()
        }
    }

    override fun inject(`object`: Any) {
        getApplicationGraph().inject(object)
    }

    override fun getApplicationGraph(): ObjectGraph {
        if (mActivityGraph == null) {
            mActivityGraph = ((Injectable) getApplication()).getApplicationGraph()
                    .plus(ActivityModule(this), UiModule())
        }
        return mActivityGraph
    }

    fun isActivityDestroyed(): Boolean {
        return mDestroyed
    }
}
