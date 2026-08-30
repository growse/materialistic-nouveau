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

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.growse.android.io.github.hidroh.materialistic.Preferences.getUsername
import com.growse.android.io.github.hidroh.materialistic.Preferences.setUsername
import com.growse.android.io.github.hidroh.materialistic.accounts.UserServices
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AccountAuthenticatorActivity() {
  @Inject lateinit var userServices: UserServices

  @Inject lateinit var accountManager: AccountManager
  private var loginButton: View? = null
  private var registerButton: View? = null
  private var usernameLayout: TextInputLayout? = null
  private var passwordLayout: TextInputLayout? = null
  private var usernameEditText: EditText? = null
  private var passwordEditText: EditText? = null
  private var mUsername: String? = null
  private var mPassword: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val username = getUsername(this)
    val addAccount = intent.getBooleanExtra(EXTRA_ADD_ACCOUNT, false)
    setContentView(R.layout.activity_login)
    usernameLayout = findViewById<View>(R.id.textinput_username) as TextInputLayout
    passwordLayout = findViewById<View>(R.id.textinput_password) as TextInputLayout
    usernameEditText = findViewById<View>(R.id.edittext_username) as EditText
    loginButton =
        findViewById<View>(R.id.login_button).apply {
          setOnClickListener(
              View.OnClickListener { _: View? ->
                if (!validate()) {
                  return@OnClickListener
                }
                isEnabled = false
                registerButton!!.isEnabled = false
                login(
                    usernameEditText!!.text.toString(),
                    passwordEditText!!.text.toString(),
                    false,
                )
              }
          )
        }
    registerButton =
        findViewById<View>(R.id.register_button).apply {
          setOnClickListener(
              View.OnClickListener { _: View? ->
                if (!validate()) {
                  return@OnClickListener
                }
                isEnabled = false
                loginButton!!.isEnabled = false
                login(
                    usernameEditText!!.text.toString().trim { it <= ' ' },
                    passwordEditText!!.text.toString().trim { it <= ' ' },
                    true,
                )
              }
          )
        }
    if (!addAccount && !TextUtils.isEmpty(username)) {
      setTitle(R.string.re_enter_password)
      usernameEditText!!.setText(username)
      registerButton!!.visibility = View.GONE
    }
    passwordEditText = findViewById<View?>(R.id.edittext_password) as EditText
  }

  override fun isDialogTheme(): Boolean {
    return true
  }

  private fun validate(): Boolean {
    usernameLayout!!.isErrorEnabled = false
    passwordLayout!!.isErrorEnabled = false
    if (usernameEditText!!.length() == 0) {
      usernameLayout!!.error = getString(R.string.username_required)
    }
    if (passwordEditText!!.length() == 0) {
      passwordLayout!!.error = getString(R.string.password_required)
    }
    return usernameEditText!!.length() > 0 && passwordEditText!!.length() > 0
  }

  private fun login(username: String, password: String?, createAccount: Boolean) {
    mUsername = username
    mPassword = password
    userServices.login(username, password, createAccount, LoginCallback(this))
  }

  fun onLoggedIn(successful: Boolean, errorMessage: String?) {
    loginButton!!.isEnabled = true
    registerButton!!.isEnabled = true
    if (successful) {
      addAccount(mUsername!!, mPassword)
      Toast.makeText(this, getString(R.string.welcome, mUsername), Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(
              this,
              if (TextUtils.isEmpty(errorMessage)) getString(R.string.login_failed)
              else errorMessage,
              Toast.LENGTH_SHORT,
          )
          .show()
    }
  }

  private fun addAccount(username: String, password: String?) {
    val account = Account(username, BuildConfig.APPLICATION_ID)
    accountManager.addAccountExplicitly(account, password, null)
    accountManager.setPassword(account, password) // for re-login with updated password
    val bundle = Bundle()
    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username)
    bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.APPLICATION_ID)
    setAccountAuthenticatorResult(bundle)
    setUsername(this, username)
    finish()
  }

  internal class LoginCallback(loginActivity: LoginActivity?) : UserServices.Callback() {
    private val mLoginActivity: WeakReference<LoginActivity?> =
        WeakReference<LoginActivity?>(loginActivity)

    override fun onDone(successful: Boolean) {
      if (mLoginActivity.get() != null && !mLoginActivity.get()!!.isActivityDestroyed) {
        mLoginActivity.get()!!.onLoggedIn(successful, null)
      }
    }

    override fun onError(throwable: Throwable?) {
      if (mLoginActivity.get() != null && !mLoginActivity.get()!!.isActivityDestroyed) {
        mLoginActivity.get()!!.onLoggedIn(false, throwable?.message)
      }
    }
  }

  companion object {
    // @JvmField so the Java callers keep seeing a static field rather than a getter
    @JvmField val EXTRA_ADD_ACCOUNT: String = LoginActivity::class.java.name + ".EXTRA_ADD_ACCOUNT"
  }
}
