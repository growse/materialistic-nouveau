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
import android.os.Bundle

/**
 * Base fragment that controls load timing depends on WIFI and visibility
 */
abstract class LazyLoadFragment : BaseFragment() {
    val EXTRA_EAGER_LOAD: String = LazyLoadFragment::class.java.name + ".EXTRA_EAGER_LOAD"
    val EXTRA_RETAIN_INSTANCE: String = WebFragment::class.java.name + ".EXTRA_RETAIN_INSTANCE"
    private const val STATE_EAGER_LOAD: String = "state:eagerLoad"
    private const val STATE_LOADED: String = "state:loaded"
    private var mActivityCreated: boolean mEagerLoad, mLoaded,? = null
    private var mNewInstance: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mNewInstance = false
    }

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setRetainInstance(getArguments().getBoolean(EXTRA_RETAIN_INSTANCE, false))
        mNewInstance = true
        if (savedInstanceState != null) {
            mEagerLoad = savedInstanceState.getBoolean(STATE_EAGER_LOAD)
            mLoaded = savedInstanceState.getBoolean(STATE_LOADED)
        } else {
            mEagerLoad = getArguments() != null && getArguments().getBoolean(EXTRA_EAGER_LOAD) ||
                    !Preferences.shouldLazyLoad(getActivity())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle) {
        super.onActivityCreated(savedInstanceState)
        mActivityCreated = true
        if (isNewInstance()) {
            eagerLoad()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_EAGER_LOAD, mEagerLoad)
        outState.putBoolean(STATE_LOADED, false); // allow re-loading on state restoration
    }

    override fun onDetach() {
        super.onDetach()
        mActivityCreated = false
    }

    fun loadNow() {
        if (mActivityCreated) {
            mEagerLoad = true
            eagerLoad()
        }
    }

    /**
     * Load data after fragment becomes visible or if WIFI is enabled
     */
    protected abstract fun load()

    protected fun isNewInstance(): Boolean {
        return !getRetainInstance() || mNewInstance
    }

    fun eagerLoad() {
        if (mEagerLoad && !mLoaded) {
            mLoaded = true
            load()
        }
    }
}
