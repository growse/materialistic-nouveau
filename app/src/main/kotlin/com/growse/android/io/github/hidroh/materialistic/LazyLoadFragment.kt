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
package com.growse.android.io.github.hidroh.materialistic

import android.content.Context
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint

/** Base fragment that controls load timing depends on WIFI and visibility */
@AndroidEntryPoint
abstract class LazyLoadFragment : BaseFragment() {
  private var eagerLoad = false
  private var loaded = false
  private var activityCreated = false
  private var newInstance = false

  /** False when this retained instance is being re-attached after a configuration change. */
  @Suppress("DEPRECATION")
  protected val isNewInstance: Boolean
    get() = !retainInstance || newInstance

  /** Load data after fragment becomes visible or if WIFI is enabled */
  protected abstract fun load()

  override fun onAttach(context: Context) {
    super.onAttach(context)
    newInstance = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = requireArguments()
    @Suppress("DEPRECATION")
    retainInstance = args.getBoolean(EXTRA_RETAIN_INSTANCE, false)
    newInstance = true
    if (savedInstanceState != null) {
      eagerLoad = savedInstanceState.getBoolean(STATE_EAGER_LOAD)
      loaded = savedInstanceState.getBoolean(STATE_LOADED)
    } else {
      eagerLoad =
          args.getBoolean(EXTRA_EAGER_LOAD) || !Preferences.shouldLazyLoad(requireActivity())
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    @Suppress("DEPRECATION") super.onActivityCreated(savedInstanceState)
    activityCreated = true
    if (isNewInstance) {
      loadIfEager()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_EAGER_LOAD, eagerLoad)
    outState.putBoolean(STATE_LOADED, false) // allow re-loading on state restoration
  }

  override fun onDetach() {
    super.onDetach()
    activityCreated = false
  }

  fun loadNow() {
    if (activityCreated) {
      eagerLoad = true
      loadIfEager()
    }
  }

  private fun loadIfEager() {
    if (eagerLoad && !loaded) {
      loaded = true
      load()
    }
  }

  companion object {
    @JvmField val EXTRA_EAGER_LOAD: String = LazyLoadFragment::class.java.name + ".EXTRA_EAGER_LOAD"
    @JvmField
    val EXTRA_RETAIN_INSTANCE: String = WebFragment::class.java.name + ".EXTRA_RETAIN_INSTANCE"
    private const val STATE_EAGER_LOAD = "state:eagerLoad"
    private const val STATE_LOADED = "state:loaded"
  }
}
