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

package io.github.hidroh.materialistic.accounts

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import android.text.TextUtils
import android.widget.Toast

import java.io.IOException
import java.net.HttpURLConnection
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.inject.Inject

import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.R
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers

open class UserServicesClient : UserServices {
    private const val BASE_WEB_URL: String = "https://news.ycombinator.com"
    private const val LOGIN_PATH: String = "login"
    private const val VOTE_PATH: String = "vote"
    private const val COMMENT_PATH: String = "comment"
    private const val SUBMIT_PATH: String = "submit"
    private const val ITEM_PATH: String = "item"
    private const val SUBMIT_POST_PATH: String = "r"
    private const val LOGIN_PARAM_ACCT: String = "acct"
    private const val LOGIN_PARAM_PW: String = "pw"
    private const val LOGIN_PARAM_CREATING: String = "creating"
    private const val LOGIN_PARAM_GOTO: String = "goto"
    private const val ITEM_PARAM_ID: String = "id"
    private const val VOTE_PARAM_ID: String = "id"
    private const val VOTE_PARAM_HOW: String = "how"
    private const val COMMENT_PARAM_PARENT: String = "parent"
    private const val COMMENT_PARAM_TEXT: String = "text"
    private const val SUBMIT_PARAM_TITLE: String = "title"
    private const val SUBMIT_PARAM_URL: String = "url"
    private const val SUBMIT_PARAM_TEXT: String = "text"
    private const val SUBMIT_PARAM_FNID: String = "fnid"
    private const val SUBMIT_PARAM_FNOP: String = "fnop"
    private const val VOTE_DIR_UP: String = "up"
    private const val DEFAULT_REDIRECT: String = "news"
    private const val CREATING_TRUE: String = "t"
    private const val DEFAULT_FNOP: String = "submit-page"
    private const val DEFAULT_SUBMIT_REDIRECT: String = "newest"
    private const val REGEX_INPUT: String = "<\\s*input[^>]*>"
    private const val REGEX_VALUE: String = "value[^\"]*\"([^\"]*)\""
    private const val REGEX_CREATE_ERROR_BODY: String = "<body>([^<]*)"
    private const val HEADER_LOCATION: String = "location"
    private const val HEADER_COOKIE: String = "cookie"
    private const val HEADER_SET_COOKIE: String = "set-cookie"
    private var mCallFactory: Call.Factory? = null
    private var mIoScheduler: Scheduler? = null

    @Inject
    constructor(callFactory: Call.Factory, ioScheduler: Scheduler) {
        mCallFactory = callFactory
        mIoScheduler = ioScheduler
    }

    override fun login(username: String, password: String, createAccount: Boolean, callback: Callback) {
        execute(postLogin(username, password, createAccount))
                .flatMap(response -> {
                    if (response.code() == HttpURLConnection.HTTP_OK) {
                        return Observable.error(UserServices.Exception(parseLoginError(response)))
                    }
                    return Observable.just(response.code() == HttpURLConnection.HTTP_MOVED_TEMP)
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError)
    }

    override fun voteUp(context: Context, itemId: String, callback: Callback): Boolean {
        Pair<String, String> credentials = AppUtils.getCredentials(context)
        if (credentials == null) {
            return false
        }
        Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show()
        execute(postVote(credentials.first, credentials.second, itemId))
                .map(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError)
        return true
    }

    override fun reply(context: Context, parentId: String, text: String, callback: Callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context)
        if (credentials == null) {
            callback.onDone(false)
            return
        }
        execute(postReply(parentId, text, credentials.first, credentials.second))
                .map(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError)
    }

    override fun submit(context: Context, title: String, content: String, isUrl: Boolean, callback: Callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context)
        if (credentials == null) {
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
        // fetch submit page with given credentials
        execute(postSubmitForm(credentials.first, credentials.second))
                .flatMap(response -> response.code() != HttpURLConnection.HTTP_MOVED_TEMP ?
                        Observable.just(response) :
                        Observable.error(IOException()))
                .flatMap(response -> {
                    try {
                        return Observable.just(String[]{
                                response.header(HEADER_SET_COOKIE),
                                response.body().string()
                        })
                    } catch (IOException e) {
                        return Observable.error(e)
                    } finally {
                        response.close()
                    }
                })
                .map(array -> {
                    array[1] = getInputValue(array[1], SUBMIT_PARAM_FNID)
                    return array
                })
                .flatMap(array -> !TextUtils.isEmpty(array[1]) ?
                        Observable.just(array) :
                        Observable.error(IOException()))
                .flatMap(array -> execute(postSubmit(title, content, isUrl, array[0], array[1])))
                .flatMap(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP ?
                        Observable.just(Uri.parse(response.header(HEADER_LOCATION))) :
                        Observable.error(IOException()))
                .flatMap(uri -> TextUtils.equals(uri.getPath(), DEFAULT_SUBMIT_REDIRECT) ?
                        Observable.just(true) :
                        Observable.error(buildException(uri)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError)
    }

    private fun postLogin(username: String, password: String, createAccount: Boolean): Request {
        FormBody.Builder formBuilder = FormBody.Builder()
                .add(LOGIN_PARAM_ACCT, username)
                .add(LOGIN_PARAM_PW, password)
                .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT)
        if (createAccount) {
            formBuilder.add(LOGIN_PARAM_CREATING, CREATING_TRUE)
        }
        return Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(LOGIN_PATH)
                        .build())
                .post(formBuilder.build())
                .build()
    }

    private fun postVote(username: String, password: String, itemId: String): Request {
        return Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(VOTE_PATH)
                        .build())
                .post(FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .add(VOTE_PARAM_ID, itemId)
                        .add(VOTE_PARAM_HOW, VOTE_DIR_UP)
                        .build())
                .build()
    }

    private fun postReply(parentId: String, text: String, username: String, password: String): Request {
        return Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(COMMENT_PATH)
                        .build())
                .post(FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .add(COMMENT_PARAM_PARENT, parentId)
                        .add(COMMENT_PARAM_TEXT, text)
                        .build())
                .build()
    }

    private fun postSubmitForm(username: String, password: String): Request {
        return Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_PATH)
                        .build())
                .post(FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .build())
                .build()
    }

    private fun postSubmit(title: String, content: String, isUrl: Boolean, cookie: String, fnid: String): Request {
        Request.Builder builder = Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_POST_PATH)
                        .build())
                .post(FormBody.Builder()
                        .add(SUBMIT_PARAM_FNID, fnid)
                        .add(SUBMIT_PARAM_FNOP, DEFAULT_FNOP)
                        .add(SUBMIT_PARAM_TITLE, title)
                        .add(isUrl ? SUBMIT_PARAM_URL : SUBMIT_PARAM_TEXT, content)
                        .build())
        if (!TextUtils.isEmpty(cookie)) {
            builder.addHeader(HEADER_COOKIE, cookie)
        }
        return builder.build()
    }

    private fun execute(request: Request): Observable<Response> {
        return Observable.defer(() -> {
            try {
                return Observable.just(mCallFactory.newCall(request).execute())
            } catch (IOException e) {
                return Observable.error(e)
            }
        }).subscribeOn(mIoScheduler)
    }

    private fun buildException(uri: Uri): Throwable {
        switch (uri.getPath()) {
            case ITEM_PATH:
                UserServices.Exception exception = UserServices.Exception(R.string.item_exist)
                String itemId = uri.getQueryParameter(ITEM_PARAM_ID)
                if (!TextUtils.isEmpty(itemId)) {
                    exception.data = AppUtils.createItemUri(itemId)
                }
                return exception
            default:
                return IOException()
        }
    }

    private fun getInputValue(html: String, name: String): String {
        // extract <input ... >
        Matcher matcherInput = Pattern.compile(REGEX_INPUT).matcher(html)
        while (matcherInput.find()) {
            String input = matcherInput.group()
            if (input.contains(name)) {
                // extract value="..."
                Matcher matcher = Pattern.compile(REGEX_VALUE).matcher(input)
                return matcher.find() ? matcher.group(1) : null; // return first match if any
            }
        }
        return null
    }

    private fun parseLoginError(response: Response): String {
        try {
            Matcher matcher = Pattern.compile(REGEX_CREATE_ERROR_BODY).matcher(response.body().string())
            return matcher.find() ? matcher.group(1).replaceAll("\\n|\\r|\\t|\\s+", " ").trim() : null
        } catch (IOException e) {
            return null
        }
    }
}
