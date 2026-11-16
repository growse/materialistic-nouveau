package com.growse.android.io.github.hidroh.materialistic.accounts

import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.growse.android.io.github.hidroh.materialistic.R
import com.growse.android.io.github.hidroh.materialistic.screens.LoginScreen
import com.growse.android.io.github.hidroh.materialistic.settings.Preferences
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.PASSWORD
import com.growse.android.io.github.hidroh.materialistic.testing.AccountsRule.Companion.USERNAME
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackend
import com.growse.android.io.github.hidroh.materialistic.testing.FakeBackendRule
import com.growse.android.io.github.hidroh.materialistic.testing.FakeHackerNewsSite
import com.growse.android.io.github.hidroh.materialistic.testing.ScenarioRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginTest : TestCase() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val backend = FakeBackendRule()

  @get:Rule(order = 2) val accounts = AccountsRule()

  @get:Rule(order = 3) val scenarios = ScenarioRule()

  private val context
    get() = device.targetContext

  @Test
  fun emptyFieldsAreFlaggedAndNothingIsSent() = run {
    step("Open login and tap login with nothing entered") {
      scenarios.launch(LoginActivity::class.java)
      LoginScreen { loginButton.click() }
    }
    step("Verify both fields are flagged and no login was attempted") {
      LoginScreen {
        usernameLayout.hasError(context.getString(R.string.username_required))
        passwordLayout.hasError(context.getString(R.string.password_required))
      }
      assertTrue(FakeBackend.postsTo("/login").isEmpty())
    }
  }

  @Test
  fun loggingInSavesTheAccountAndSignsIn() = run {
    step("HN accepts the login") { FakeHackerNewsSite.loginSucceeds() }
    val scenario = scenarios.launch(LoginActivity::class.java)
    enterCredentials()
    step("Log in") { LoginScreen { loginButton.click() } }
    step("Verify login closes, having saved the account and signed in with it") {
      flakySafely { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
      assertEquals(mapOf(USERNAME to PASSWORD), accounts.accounts())
      assertEquals(USERNAME, Preferences.getUsername(context))
    }
    step("Verify the credentials were sent, without asking to create an account") {
      val form = FakeBackend.postsTo("/login").single().form
      assertEquals(USERNAME, form["acct"])
      assertEquals(PASSWORD, form["pw"])
      assertNull(form["creating"])
    }
  }

  @Test
  fun aRejectedLoginKeepsTheFormOpenAndSavesNothing() = run {
    step("HN turns the login away") { FakeHackerNewsSite.loginFails() }
    scenarios.launch(LoginActivity::class.java)
    enterCredentials()
    step("Log in") { LoginScreen { loginButton.click() } }
    step("Verify the form stays open for another try, with no account saved") {
      LoginScreen { loginButton.isEnabled() }
      assertTrue(accounts.accounts().isEmpty())
      assertEquals("", Preferences.getUsername(context))
    }
  }

  @Test
  fun creatingAnAccountAsksHnToCreateIt() = run {
    step("HN accepts the new account") { FakeHackerNewsSite.loginSucceeds() }
    scenarios.launch(LoginActivity::class.java)
    enterCredentials()
    step("Create the account") { LoginScreen { registerButton.click() } }
    step("Verify HN was asked to create it") {
      flakySafely { assertEquals("t", FakeBackend.postsTo("/login").single().form["creating"]) }
    }
  }

  @Test
  fun aStaleSignInAsksOnlyForThePassword() = run {
    step("Be signed in as a user whose account has gone") {
      Preferences.setUsername(context, USERNAME)
    }
    step("Open login") { scenarios.launch(LoginActivity::class.java) }
    step("Verify it asks to re-enter the password for that user, with no option to register") {
      LoginScreen {
        title(context.getString(R.string.re_enter_password)).isVisible()
        username.hasText(USERNAME)
        registerButton.isGone()
      }
    }
  }

  private fun TestContext<Unit>.enterCredentials() =
      step("Enter a username and password") {
        LoginScreen {
          username.typeText(USERNAME)
          password.typeText(PASSWORD)
        }
        closeSoftKeyboard()
      }
}
