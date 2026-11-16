package com.growse.android.io.github.hidroh.materialistic.testing

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.growse.android.io.github.hidroh.materialistic.BuildConfig
import com.growse.android.io.github.hidroh.materialistic.settings.Preferences
import org.junit.rules.ExternalResource

/**
 * Starts each test signed out with no HN accounts on the device, and removes any the test added.
 * [signIn] adds one and signs in with it, as logging in through the app would.
 */
class AccountsRule : ExternalResource() {
  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  private val accountManager: AccountManager
    get() = AccountManager.get(context)

  override fun before() = clear()

  override fun after() = clear()

  fun signIn(username: String = USERNAME, password: String = PASSWORD) {
    accountManager.addAccountExplicitly(
        Account(username, BuildConfig.APPLICATION_ID),
        password,
        null,
    )
    Preferences.setUsername(context, username)
  }

  /** The accounts on the device, by name, with their saved passwords. */
  fun accounts(): Map<String, String?> =
      accountManager.getAccountsByType(BuildConfig.APPLICATION_ID).associate {
        it.name to accountManager.getPassword(it)
      }

  private fun clear() {
    Preferences.setUsername(context, null)
    accountManager.getAccountsByType(BuildConfig.APPLICATION_ID).forEach {
      accountManager.removeAccountExplicitly(it)
    }
  }

  companion object {
    const val USERNAME = "alice"
    const val PASSWORD = "hunter2"
  }
}
