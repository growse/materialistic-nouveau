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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseFragment : Fragment() {
  @JvmField protected val menuTintDelegate: MenuTintDelegate = MenuTintDelegate()
  var isAttached: Boolean = false
    private set

  // Unlike setHasOptionsMenu(), a MenuProvider ignores menu visibility, so check it here to
  // keep off-screen ViewPager pages from contributing to the options menu.
  private val menuProvider =
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          if (isMenuVisible) {
            createOptionsMenu(menu, menuInflater)
            menuTintDelegate.onOptionsMenuCreated(menu)
          }
        }

        override fun onPrepareMenu(menu: Menu) {
          if (isMenuVisible) {
            prepareOptionsMenu(menu)
          }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            isMenuVisible && optionsItemSelected(menuItem)
      }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    this.isAttached = true
    menuTintDelegate.onActivityCreated(context)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
  }

  override fun setMenuVisibility(menuVisible: Boolean) {
    val changed = menuVisible != isMenuVisible
    super.setMenuVisibility(menuVisible)
    if (changed && isAdded && !isHidden) {
      requireActivity().invalidateMenu()
    }
  }

  override fun onDetach() {
    super.onDetach()
    this.isAttached = false
  }

  protected open fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    // override to create options menu
  }

  protected open fun prepareOptionsMenu(menu: Menu) {
    // override to prepare options menu
  }

  protected open fun optionsItemSelected(item: MenuItem): Boolean {
    // override to handle options menu item selection
    return false
  }
}
