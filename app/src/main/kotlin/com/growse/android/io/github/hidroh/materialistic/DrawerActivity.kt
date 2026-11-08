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

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class DrawerActivity : ThemedActivity() {
  @Inject lateinit var alertDialogBuilder: AlertDialogBuilder<*>

  private lateinit var drawerToggle: ActionBarDrawerToggle
  private lateinit var drawerLayout: DrawerLayout
  private lateinit var drawer: View
  private lateinit var drawerAccount: TextView
  private lateinit var drawerLogout: View
  private lateinit var drawerUser: View
  private var pendingNavigation: Class<out Activity>? = null
  private var pendingNavigationExtras: Bundle? = null

  private val backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (drawerLayout.isDrawerOpen(drawer)) {
            drawerLayout.closeDrawers()
          } else if (isTaskRoot && Preferences.isLaunchScreenLast(this@DrawerActivity)) {
            moveTaskToBack(true)
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
          }
        }
      }
  private val loginListener = OnSharedPreferenceChangeListener { _, key ->
    if (key == getString(R.string.pref_username)) {
      bindUsername()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    super.setContentView(R.layout.activity_drawer)
    drawer = findViewById(R.id.drawer)
    drawerLayout = findViewById(R.id.drawer_layout)
    drawerAccount = findViewById(R.id.drawer_account)
    drawerLogout = findViewById(R.id.drawer_logout)
    drawerUser = findViewById(R.id.drawer_user)
    drawerToggle =
        object :
            ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
          override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            if (drawerView == drawer) {
              startPendingNavigation()
            }
          }
        }
    drawerLayout.addDrawerListener(drawerToggle)
    PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(loginListener)
    setUpDrawer()
    bindUsername()
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    drawerToggle.syncState()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    drawerToggle.onConfigurationChanged(newConfig)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
      drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)

  override fun onDestroy() {
    super.onDestroy()
    drawerLayout.removeDrawerListener(drawerToggle)
    PreferenceManager.getDefaultSharedPreferences(this)
        .unregisterOnSharedPreferenceChangeListener(loginListener)
  }

  /** Inflates subclass layouts into the drawer layout, behind the drawer itself. */
  override fun setContentView(layoutResID: Int) {
    val drawerLayout = findViewById<ViewGroup>(R.id.drawer_layout)
    drawerLayout.addView(layoutInflater.inflate(layoutResID, drawerLayout, false), 0)
  }

  private fun setUpDrawer() {
    drawerAccount.setOnClickListener { showLogin() }
    drawerLogout.setOnClickListener { confirmLogout() }
    val moreContainer = findViewById<View>(R.id.drawer_more_container)
    val moreToggle = findViewById<TextView>(R.id.drawer_more)
    moreToggle.setOnClickListener {
      val expand = !moreContainer.isVisible
      @AttrRes
      val textColor =
          if (expand) android.R.attr.textColorSecondary else android.R.attr.textColorTertiary
      @DrawableRes
      val icon =
          if (expand) R.drawable.ic_expand_less_white_24dp else R.drawable.ic_expand_more_white_24dp
      moreToggle.setTextColor(
          ContextCompat.getColor(this, AppUtils.getThemedResId(this, textColor)),
      )
      moreToggle.setCompoundDrawablesWithIntrinsicBounds(
          R.drawable.ic_dummy_transparent_24dp,
          0,
          icon,
          0,
      )
      moreContainer.isVisible = expand
    }
    mapOf(
            R.id.drawer_list to ListActivity::class.java,
            R.id.drawer_best to BestActivity::class.java,
            R.id.drawer_popular to PopularActivity::class.java,
            R.id.drawer_new to NewActivity::class.java,
            R.id.drawer_show to ShowActivity::class.java,
            R.id.drawer_ask to AskActivity::class.java,
            R.id.drawer_job to JobsActivity::class.java,
            R.id.drawer_settings to SettingsActivity::class.java,
            R.id.drawer_favorite to FavoriteActivity::class.java,
            R.id.drawer_submit to SubmitActivity::class.java,
            R.id.drawer_feedback to FeedbackActivity::class.java,
        )
        .forEach { (id, activityClass) ->
          findViewById<View>(id).setOnClickListener { navigate(activityClass) }
        }
    drawerUser.setOnClickListener {
      navigate(
          UserActivity::class.java,
          bundleOf(UserActivity.EXTRA_USERNAME to Preferences.getUsername(this)),
      )
    }
  }

  @SuppressLint("MissingPermission")
  private fun showLogin() {
    val accounts = AccountManager.get(this).getAccountsByType(BuildConfig.APPLICATION_ID)
    if (accounts.isEmpty()) { // no accounts, ask to login or re-login
      startActivity(Intent(this, LoginActivity::class.java))
    } else { // has accounts, show account chooser regardless of login status
      AppUtils.showAccountChooser(this, alertDialogBuilder, accounts)
    }
  }

  private fun confirmLogout() {
    alertDialogBuilder
        .init(this)
        .setMessage(R.string.logout_confirm)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> Preferences.setUsername(this, null) }
        .show()
  }

  /** Queues navigation to [activityClass] until the drawer has finished closing. */
  private fun navigate(activityClass: Class<out Activity>, extras: Bundle? = null) {
    pendingNavigation = if (javaClass != activityClass) activityClass else null
    pendingNavigationExtras = extras
    drawerLayout.closeDrawers()
  }

  private fun startPendingNavigation() {
    val activityClass = pendingNavigation ?: return
    val intent = Intent(this, activityClass)
    pendingNavigationExtras?.let { intent.putExtras(it) }
    // TODO M bug https://code.google.com/p/android/issues/detail?id=193822
    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    startActivity(intent)
    pendingNavigation = null
    pendingNavigationExtras = null
  }

  private fun bindUsername() {
    val username = Preferences.getUsername(this)
    val loggedIn = username.isNotEmpty()
    if (loggedIn) {
      drawerAccount.text = username
    } else {
      drawerAccount.setText(R.string.login)
    }
    drawerLogout.isVisible = loggedIn
    drawerUser.isVisible = loggedIn
  }
}
