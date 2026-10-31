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
package com.growse.android.io.github.hidroh.materialistic.accounts

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.growse.android.io.github.hidroh.materialistic.BuildConfig
import com.growse.android.io.github.hidroh.materialistic.Preferences
import com.growse.android.io.github.hidroh.materialistic.R
import java.io.IOException
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_OK
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers

class UserServicesClient(
    private val callFactory: Call.Factory,
    private val ioScheduler: Scheduler,
    private val getCredentials: (Context) -> Pair<String, String>? = ::accountCredentials,
) : UserServices {
  override fun login(
      username: String,
      password: String,
      createAccount: Boolean,
      callback: UserServices.Callback,
  ) {
    val form =
        FormBody.Builder()
            .add(LOGIN_PARAM_ACCT, username)
            .add(LOGIN_PARAM_PW, password)
            .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT)
            .apply { if (createAccount) add(LOGIN_PARAM_CREATING, CREATING_TRUE) }
            .build()
    execute(post(LOGIN_PATH, form))
        .map { response ->
          response.use {
            // a failed login re-renders the form (200) with the reason in the body
            if (it.code == HTTP_OK) throw UserServices.Exception(parseLoginError(it))
            it.code == HTTP_MOVED_TEMP
          }
        }
        .deliverTo(callback)
  }

  override fun voteUp(context: Context, itemId: String, callback: UserServices.Callback): Boolean {
    val (username, password) = getCredentials(context) ?: return false
    Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show()
    val form =
        FormBody.Builder()
            .add(LOGIN_PARAM_ACCT, username)
            .add(LOGIN_PARAM_PW, password)
            .add(VOTE_PARAM_ID, itemId)
            .add(VOTE_PARAM_HOW, VOTE_DIR_UP)
            .build()
    execute(post(VOTE_PATH, form)).map { it.use { it.code == HTTP_MOVED_TEMP } }.deliverTo(callback)
    return true
  }

  override fun reply(
      context: Context,
      parentId: String,
      text: String,
      callback: UserServices.Callback,
  ) {
    val (username, password) =
        getCredentials(context)
            ?: run {
              callback.onDone(false)
              return
            }
    val form =
        FormBody.Builder()
            .add(LOGIN_PARAM_ACCT, username)
            .add(LOGIN_PARAM_PW, password)
            .add(COMMENT_PARAM_PARENT, parentId)
            .add(COMMENT_PARAM_TEXT, text)
            .build()
    execute(post(COMMENT_PATH, form))
        .map { it.use { it.code == HTTP_MOVED_TEMP } }
        .deliverTo(callback)
  }

  override fun submit(
      context: Context,
      title: String,
      content: String,
      isUrl: Boolean,
      callback: UserServices.Callback,
  ) {
    val (username, password) =
        getCredentials(context)
            ?: run {
              callback.onDone(false)
              return
            }
    /*
     The flow:
     POST /submit with acc, pw
      if 302 to /login, considered failed
     POST /r with fnid, fnop, title, url or text
      if 302 to /newest, considered successful
      if 302 to /x, considered error, maybe duplicate or invalid input
      if 200 or anything else, considered error
    */
    val loginForm =
        FormBody.Builder().add(LOGIN_PARAM_ACCT, username).add(LOGIN_PARAM_PW, password).build()
    // fetch submit page with given credentials
    execute(post(SUBMIT_PATH, loginForm))
        .map { response ->
          response.use {
            if (it.code == HTTP_MOVED_TEMP) throw IOException()
            it.header(HEADER_SET_COOKIE) to getInputValue(it.body.string(), SUBMIT_PARAM_FNID)
          }
        }
        .flatMap { (cookie, fnid) ->
          if (fnid.isNullOrEmpty()) {
            Observable.error(IOException())
          } else {
            val submitForm =
                FormBody.Builder()
                    .add(SUBMIT_PARAM_FNID, fnid)
                    .add(SUBMIT_PARAM_FNOP, DEFAULT_FNOP)
                    .add(SUBMIT_PARAM_TITLE, title)
                    .add(if (isUrl) SUBMIT_PARAM_URL else SUBMIT_PARAM_TEXT, content)
                    .build()
            execute(post(SUBMIT_POST_PATH, submitForm, cookie))
          }
        }
        .map { response ->
          val redirect = response.use {
            if (it.code != HTTP_MOVED_TEMP) throw IOException()
            it.header(HEADER_LOCATION)?.let(BASE_URL::resolve) ?: throw IOException()
          }
          if (redirect.pathSegments != listOf(DEFAULT_SUBMIT_REDIRECT)) {
            throw buildException(redirect)
          }
          true
        }
        .deliverTo(callback)
  }

  private fun post(path: String, body: FormBody, cookie: String? = null): Request =
      Request.Builder()
          .url(BASE_URL.newBuilder().addPathSegment(path).build())
          .post(body)
          .apply { if (!cookie.isNullOrEmpty()) addHeader(HEADER_COOKIE, cookie) }
          .build()

  private fun execute(request: Request): Observable<Response> =
      Observable.fromCallable { callFactory.newCall(request).execute() }.subscribeOn(ioScheduler)

  private fun Observable<Boolean>.deliverTo(callback: UserServices.Callback) {
    observeOn(AndroidSchedulers.mainThread())
        .subscribe({ callback.onDone(it) }, { callback.onError(it) })
  }

  private fun buildException(redirect: HttpUrl): Throwable =
      when (redirect.pathSegments) {
        listOf(ITEM_PATH) ->
            UserServices.Exception(R.string.item_exist).apply {
              itemId = redirect.queryParameter(ITEM_PARAM_ID)?.takeIf { it.isNotEmpty() }
            }
        else -> IOException()
      }

  /** Returns the value of the first `<input>` in [html] whose markup mentions [name]. */
  private fun getInputValue(html: String, name: String): String? =
      INPUT_REGEX.findAll(html)
          .firstOrNull { name in it.value }
          ?.let { VALUE_REGEX.find(it.value)?.groupValues?.get(1) }

  private fun parseLoginError(response: Response): String? =
      try {
        ERROR_BODY_REGEX.find(response.body.string())
            ?.groupValues
            ?.get(1)
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim()
      } catch (_: IOException) {
        null
      }

  companion object {
    private val BASE_URL = "https://news.ycombinator.com".toHttpUrl()
    private const val LOGIN_PATH = "login"
    private const val VOTE_PATH = "vote"
    private const val COMMENT_PATH = "comment"
    private const val SUBMIT_PATH = "submit"
    private const val ITEM_PATH = "item"
    private const val SUBMIT_POST_PATH = "r"
    private const val LOGIN_PARAM_ACCT = "acct"
    private const val LOGIN_PARAM_PW = "pw"
    private const val LOGIN_PARAM_CREATING = "creating"
    private const val LOGIN_PARAM_GOTO = "goto"
    private const val ITEM_PARAM_ID = "id"
    private const val VOTE_PARAM_ID = "id"
    private const val VOTE_PARAM_HOW = "how"
    private const val COMMENT_PARAM_PARENT = "parent"
    private const val COMMENT_PARAM_TEXT = "text"
    private const val SUBMIT_PARAM_TITLE = "title"
    private const val SUBMIT_PARAM_URL = "url"
    private const val SUBMIT_PARAM_TEXT = "text"
    private const val SUBMIT_PARAM_FNID = "fnid"
    private const val SUBMIT_PARAM_FNOP = "fnop"
    private const val VOTE_DIR_UP = "up"
    private const val DEFAULT_REDIRECT = "news"
    private const val CREATING_TRUE = "t"
    private const val DEFAULT_FNOP = "submit-page"
    private const val DEFAULT_SUBMIT_REDIRECT = "newest"
    private val INPUT_REGEX = Regex("<\\s*input[^>]*>")
    private val VALUE_REGEX = Regex("value[^\"]*\"([^\"]*)\"")
    private val ERROR_BODY_REGEX = Regex("<body>([^<]*)")
    private val WHITESPACE_REGEX = Regex("\\n|\\r|\\t|\\s+")
    private const val HEADER_LOCATION = "location"
    private const val HEADER_COOKIE = "cookie"
    private const val HEADER_SET_COOKIE = "set-cookie"
  }
}

/** Looks up the logged-in user's name and stored password, or null if either is missing. */
@SuppressLint("MissingPermission")
private fun accountCredentials(context: Context): Pair<String, String>? {
  val username = Preferences.getUsername(context)
  if (username.isEmpty()) {
    return null
  }
  val accountManager = AccountManager.get(context)
  val account =
      accountManager.getAccountsByType(BuildConfig.APPLICATION_ID).firstOrNull {
        it.name == username
      } ?: return null
  val password = accountManager.getPassword(account) ?: return null
  return username to password
}
