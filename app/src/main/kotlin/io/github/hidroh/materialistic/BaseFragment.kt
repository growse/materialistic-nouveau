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
import androidx.fragment.app.Fragment
import android.view.Menu
import android.view.MenuInflater

/**
 * Base fragment which performs injection using parent's activity object graphs if any
 */
abstract class BaseFragment : Fragment() {
    protected val mMenuTintDelegate: MenuTintDelegate = new MenuTintDelegate()
    private var mAttached: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mAttached = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle) {
        super.onActivityCreated(savedInstanceState)
        if (getActivity() is Injectable) {
            ((Injectable) getActivity()).inject(this)
        }
        mMenuTintDelegate.onActivityCreated(getActivity())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAttached()) { // TODO http://b.android.com/80783
            createOptionsMenu(menu, inflater)
            mMenuTintDelegate.onOptionsMenuCreated(menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (isAttached()) { // TODO http://b.android.com/80783
            prepareOptionsMenu(menu)
        }
    }

    override fun onDetach() {
        super.onDetach()
        mAttached = false
    }

    fun isAttached(): Boolean {
        return mAttached
    }

    protected fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // override to create options menu
    }

    protected fun prepareOptionsMenu(menu: Menu) {
        // override to prepare options menu
    }
}
