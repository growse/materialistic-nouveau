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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.annotation.StyleRes
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.widget.NestedScrollView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.growse.android.io.github.hidroh.materialistic.AppUtils.SystemUiHelper
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.BackInterceptor
import com.growse.android.io.github.hidroh.materialistic.KeyDelegate.NestedScrollViewHelper
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.getReadabilityTypeface
import com.growse.android.io.github.hidroh.materialistic.Preferences.Theme.resolvePreferredReadabilityTextSize
import com.growse.android.io.github.hidroh.materialistic.Preferences.getReadabilityLineHeight
import com.growse.android.io.github.hidroh.materialistic.data.FileDownloader
import com.growse.android.io.github.hidroh.materialistic.data.FileDownloader.FileDownloaderCallback
import com.growse.android.io.github.hidroh.materialistic.data.HackerNewsClient
import com.growse.android.io.github.hidroh.materialistic.data.Item
import com.growse.android.io.github.hidroh.materialistic.data.ItemManager
import com.growse.android.io.github.hidroh.materialistic.data.ItemManagerQualifiers
import com.growse.android.io.github.hidroh.materialistic.data.ReadabilityClient
import com.growse.android.io.github.hidroh.materialistic.data.ResponseListener
import com.growse.android.io.github.hidroh.materialistic.data.WebItem
import com.growse.android.io.github.hidroh.materialistic.widget.AdBlockWebViewClient
import com.growse.android.io.github.hidroh.materialistic.widget.CacheableWebView.ArchiveClient
import com.growse.android.io.github.hidroh.materialistic.widget.PopupMenu
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named
import okhttp3.Call

@AndroidEntryPoint
class WebFragment : LazyLoadFragment(), Scrollable, BackInterceptor {
  @Inject @field:Named(ItemManagerQualifiers.HN) lateinit var itemManager: ItemManager
  @Inject lateinit var popupMenu: PopupMenu
  @Inject lateinit var readabilityClient: ReadabilityClient
  @Inject lateinit var fileDownloader: FileDownloader

  private val preferenceObservable = Preferences.Observable()
  private val receiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
          setFullscreen(intent.getBooleanExtra(EXTRA_FULLSCREEN, false))
        }
      }

  // views are kept across configuration changes when the instance is retained
  private lateinit var fragmentView: View
  private lateinit var webView: WebView
  private lateinit var scrollView: NestedScrollView
  private lateinit var fullscreenView: ViewGroup
  private lateinit var scrollViewContent: ViewGroup
  private lateinit var buttonRefresh: ImageButton
  private lateinit var controls: ViewSwitcher
  private lateinit var editText: EditText
  private lateinit var buttonMore: View
  private lateinit var buttonNext: View
  private lateinit var progressBar: ProgressBar
  private lateinit var scrollableHelper: NestedScrollViewHelper
  private lateinit var systemUiHelper: SystemUiHelper

  private lateinit var item: WebItem
  private var fullscreen = false
  private var isPdf = false
  private var content: String? = null
  private var isHackerNewsUrl = false
  private var empty = false
  private var readability = false
  private var pdfBridge: PdfAndroidJavascriptBridge? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    preferenceObservable.subscribe(
        context,
        { _, contextChanged -> onPreferenceChanged(contextChanged) },
        R.string.pref_readability_font,
        R.string.pref_readability_line_height,
        R.string.pref_readability_text_size,
    )
    LocalBroadcastManager.getInstance(context)
        .registerReceiver(receiver, IntentFilter(ACTION_FULLSCREEN))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      fullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN, false)
      content = savedInstanceState.getString(STATE_CONTENT)
      empty = savedInstanceState.getBoolean(STATE_EMPTY, false)
      readability = savedInstanceState.getBoolean(STATE_READABILITY, false)
      item = BundleCompat.getParcelable(savedInstanceState, EXTRA_ITEM, WebItem::class.java)!!
    } else {
      readability =
          Preferences.getDefaultStoryView(requireActivity()) ==
              Preferences.StoryViewMode.Readability
      item = BundleCompat.getParcelable(requireArguments(), EXTRA_ITEM, WebItem::class.java)!!
    }
    isHackerNewsUrl = isHackerNewsUrl(item)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    if (isNewInstance) {
      fragmentView = inflater.inflate(R.layout.fragment_web, container, false)
      with(fragmentView) {
        fullscreenView = findViewById(R.id.fullscreen)
        scrollViewContent = findViewById(R.id.scroll_view_content)
        scrollView = findViewById(R.id.nested_scroll_view)
        controls = findViewById(R.id.control_switcher)
        webView = findViewById(R.id.web_view)
        buttonRefresh = findViewById(R.id.button_refresh)
        buttonMore = findViewById(R.id.button_more)
        buttonNext = findViewById(R.id.button_next)
        editText = findViewById(R.id.edittext)
        progressBar = findViewById(R.id.progress)
      }
      buttonNext.isEnabled = false
      setUpWebControls(fragmentView)
      setUpWebView()
    }
    return fragmentView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (isNewInstance) {
      scrollableHelper = NestedScrollViewHelper(scrollView)
      systemUiHelper = SystemUiHelper(requireActivity().window)
      systemUiHelper.setEnabled(!resources.getBoolean(R.bool.multi_pane))
      if (fullscreen) {
        setFullscreen(true)
      }
    }
  }

  override fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_article, menu)
  }

  override fun prepareOptionsMenu(menu: Menu) {
    val menuReadability = menu.findItem(R.id.menu_readability)
    menuReadability.isVisible = modeToggleEnabled()
    menuTintDelegate.setIcon(
        menuReadability,
        if (readability) R.drawable.ic_web_black_24dp
        else R.drawable.ic_chrome_reader_mode_black_24dp,
    )
    menuReadability.setTitle(if (readability) R.string.article else R.string.readability)
    menu.findItem(R.id.menu_font_options).isVisible = fontEnabled()
  }

  override fun optionsItemSelected(item: MenuItem): Boolean =
      when (item.itemId) {
        R.id.menu_font_options -> {
          showPreferences()
          true
        }
        R.id.menu_readability -> {
          readability = !readability
          load()
          true
        }
        else -> super.optionsItemSelected(item)
      }

  override fun onResume() {
    super.onResume()
    webView.onResume()
    webView.resumeTimers()
  }

  override fun onStop() {
    super.onStop()
    webView.onPause()
    webView.pauseTimers()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_FULLSCREEN, fullscreen)
    outState.putString(STATE_CONTENT, content)
    outState.putParcelable(EXTRA_ITEM, item)
    outState.putBoolean(STATE_EMPTY, empty)
    outState.putBoolean(STATE_READABILITY, readability)
  }

  override fun onDestroy() {
    super.onDestroy()
    pdfBridge?.cleanUp()
    if (::webView.isInitialized) {
      webView.destroy()
    }
  }

  override fun onDetach() {
    super.onDetach()
    preferenceObservable.unsubscribe(requireActivity())
    LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receiver)
  }

  override fun scrollToTop() {
    if (fullscreen) {
      webView.pageUp(true)
    } else {
      scrollableHelper.scrollToTop()
    }
  }

  override fun scrollToNext(): Boolean =
      if (fullscreen) {
        webView.pageDown(false)
        true
      } else {
        scrollableHelper.scrollToNext()
      }

  override fun scrollToPrevious(): Boolean =
      if (fullscreen) {
        webView.pageUp(false)
        true
      } else {
        scrollableHelper.scrollToPrevious()
      }

  override fun onBackPressed(): Boolean {
    if (webView.canGoBack()) {
      webView.goBack()
      return true
    }
    return false
  }

  override fun load() {
    webView.visibility = View.INVISIBLE
    when {
      isHackerNewsUrl -> bindContent()
      readability && !empty -> if (content.isNullOrEmpty()) parse() else loadContent()
      else -> loadUrl()
    }
  }

  private fun loadUrl() {
    setWebSettings(true)
    reloadUrl(item.url)
  }

  @SuppressLint("AddJavascriptInterface")
  private fun reloadUrl(url: String, pdfFilePath: String? = null) {
    isPdf = false
    pdfBridge?.let {
      it.cleanUp()
      webView.removeJavascriptInterface(PDF_BRIDGE_NAME)
    }
    if (pdfFilePath != null && url == PDF_LOADER_URL) {
      setProgress(80)
      isPdf = true
      val bridge =
          PdfAndroidJavascriptBridge(
              pdfFilePath,
              object : PdfAndroidJavascriptBridge.Callbacks {
                override fun onFailure() {
                  offerExternalApp()
                  setProgress(100)
                }

                override fun onLoad() {
                  setProgress(100)
                }
              },
          )
      pdfBridge = bridge
      webView.addJavascriptInterface(bridge, PDF_BRIDGE_NAME)
      webView.setInitialScale(1)
    }
    webView.reloadUrl(url)
  }

  private fun loadContent() {
    setWebSettings(false)
    webView.reloadHtml(wrapHtml(requireActivity(), content))
  }

  private fun parse() {
    progressBar.progress = DEFAULT_PROGRESS
    readabilityClient.parse(item.id, item.url, ReadabilityCallback(this))
  }

  private fun bindContent() {
    val item = item
    if (item is Item) {
      content = item.text
      loadContent()
    } else {
      itemManager.getItem(item.id, ItemManager.MODE_DEFAULT, ItemResponseListener(this))
    }
  }

  private fun fontEnabled(): Boolean = readability && !empty && !content.isNullOrEmpty()

  private fun modeToggleEnabled(): Boolean = !isHackerNewsUrl && !webView.canGoBack()

  private fun setUpWebControls(view: View) {
    view.findViewById<View>(R.id.toolbar_web).setOnClickListener { scrollToTop() }
    view.findViewById<View>(R.id.button_back).setOnClickListener { webView.goBack() }
    view.findViewById<View>(R.id.button_forward).setOnClickListener { webView.goForward() }
    view.findViewById<View>(R.id.button_clear).setOnClickListener {
      systemUiHelper.setFullscreen(true)
      reset()
      controls.showNext()
    }
    view.findViewById<View>(R.id.button_find).setOnClickListener {
      editText.requestFocus()
      toggleSoftKeyboard(true)
      controls.showNext()
    }
    buttonRefresh.setOnClickListener {
      if (webView.progress < 100) {
        webView.stopLoading()
      } else {
        webView.reload()
      }
    }
    view.findViewById<View>(R.id.button_exit).setOnClickListener {
      LocalBroadcastManager.getInstance(requireActivity())
          .sendBroadcast(Intent(ACTION_FULLSCREEN).putExtra(EXTRA_FULLSCREEN, false))
    }
    buttonNext.setOnClickListener { webView.findNext(true) }
    buttonMore.setOnClickListener {
      popupMenu
          .create(requireActivity(), buttonMore, Gravity.NO_GRAVITY)
          .inflate(R.menu.menu_web)
          .setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
              R.id.menu_font_options -> showPreferences()
              R.id.menu_zoom_in -> webView.zoomIn()
              R.id.menu_zoom_out -> webView.zoomOut()
              else -> return@setOnMenuItemClickListener false
            }
            true
          }
          .setMenuItemVisible(R.id.menu_font_options, fontEnabled())
          .show()
    }
    editText.setOnEditorActionListener { _, _, _ ->
      findInPage()
      true
    }
  }

  private fun setUpWebView() {
    webView.setBackgroundColor(Color.TRANSPARENT)
    webView.webViewClient =
        object : AdBlockWebViewClient(Preferences.adBlockEnabled(requireActivity())) {
          override fun onPageStarted(
              view: android.webkit.WebView?,
              url: String?,
              favicon: Bitmap?,
          ) {
            super.onPageStarted(view, url, favicon)
            activity?.invalidateOptionsMenu()
          }

          override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
            super.onPageFinished(view, url)
            activity?.invalidateOptionsMenu()
          }
        }
    webView.webChromeClient =
        object : ArchiveClient() {
          override fun onProgressChanged(view: android.webkit.WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (!isPdf) {
              setProgress(newProgress)
            }
          }
        }
    webView.setDownloadListener { _, _, _, mimetype, _ ->
      if (activity == null) {
        return@setDownloadListener
      }
      if (mimetype == PDF_MIME_TYPE) {
        setProgress(10)
        isPdf = true
        downloadFileAndRenderPdf()
      } else {
        offerExternalApp()
      }
    }
    AppUtils.toggleWebViewZoom(webView.settings, false)
  }

  private fun offerExternalApp() {
    // Don't gate on resolveActivity(): package visibility hides apps that only claim
    // specific hosts (e.g. YouTube for youtube.com), so it returns null for them.
    val intent = Intent(Intent.ACTION_VIEW, item.url?.toUri())
    webView.visibility = View.GONE
    requireActivity().findViewById<View>(R.id.empty).visibility = View.VISIBLE
    requireActivity().findViewById<View>(R.id.download_button).setOnClickListener {
      try {
        startActivity(intent)
      } catch (_: ActivityNotFoundException) {}
    }
  }

  private fun setProgress(progress: Int) {
    progressBar.progress = progress
    progressBar.visibility = if (progress == 100) View.GONE else View.VISIBLE
    buttonRefresh.setImageResource(
        if (progress == 100) R.drawable.ic_refresh_white_24dp else R.drawable.ic_clear_white_24dp,
    )
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun setWebSettings(isRemote: Boolean) {
    readability = !isRemote
    webView.setBackgroundColor(if (isRemote) Color.WHITE else Color.TRANSPARENT)
    webView.settings.loadWithOverviewMode = isRemote
    webView.settings.useWideViewPort = isRemote
    webView.settings.javaScriptEnabled = true
    requireActivity().invalidateOptionsMenu()
  }

  private fun setFullscreen(isFullscreen: Boolean) {
    if (view == null) {
      return
    }
    fullscreen = isFullscreen
    controls.visibility = if (isFullscreen) View.VISIBLE else View.GONE
    AppUtils.toggleWebViewZoom(webView.settings, isFullscreen)
    val params = webView.layoutParams
    if (isFullscreen) {
      scrollView.removeView(scrollViewContent)
      webView.scrollTo(scrollView.scrollX, scrollView.scrollY)
      fullscreenView.addView(scrollViewContent)
      params.height = ViewGroup.LayoutParams.MATCH_PARENT
    } else {
      reset()
      // We'll zoom out until it returns false, which means it has min zoom level.
      // It's quite dangerous piece of code - potentially could lead to infinite loop,
      // so let's add some reasonable limit just in case
      var i = 0
      while (webView.zoomOut() && i < 30) {
        i++
      }
      fullscreenView.removeView(scrollViewContent)
      scrollView.addView(scrollViewContent)
      scrollView.post { scrollView.scrollTo(webView.scrollX, webView.scrollY) }
      params.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }
    webView.layoutParams = params
  }

  private fun showPreferences() {
    PopupSettingsFragment()
        .apply {
          arguments =
              Bundle().apply {
                putInt(PopupSettingsFragment.EXTRA_TITLE, R.string.font_options)
                putIntArray(
                    PopupSettingsFragment.EXTRA_XML_PREFERENCES,
                    intArrayOf(R.xml.preferences_readability),
                )
              }
        }
        .show(parentFragmentManager, PopupSettingsFragment::class.java.name)
  }

  private fun onPreferenceChanged(contextChanged: Boolean) {
    if (!contextChanged) {
      load()
    }
  }

  private fun reset() {
    editText.text = null
    buttonNext.isEnabled = false
    toggleSoftKeyboard(false)
    webView.clearMatches()
  }

  private fun findInPage() {
    val query = editText.text.toString().trim()
    if (query.isEmpty()) {
      return
    }
    webView.setFindListener { _, numberOfMatches, isDoneCounting ->
      if (isDoneCounting) {
        handleFindResults(numberOfMatches)
      }
    }
    webView.findAllAsync(query)
  }

  private fun handleFindResults(numberOfMatches: Int) {
    buttonNext.isEnabled = numberOfMatches > 0
    if (numberOfMatches == 0) {
      Toast.makeText(context, R.string.no_matches, Toast.LENGTH_SHORT).show()
    } else {
      toggleSoftKeyboard(false)
    }
  }

  private fun toggleSoftKeyboard(visible: Boolean) {
    val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (visible) {
      imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    } else {
      imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }
  }

  private fun onParsed(content: String?) {
    if (!isAttached) {
      return
    }
    this.content = content
    if (!content.isNullOrEmpty()) {
      loadContent()
    } else {
      empty = true
      if (readability) {
        Toast.makeText(activity, R.string.readability_failed, Toast.LENGTH_SHORT).show()
      }
      loadUrl()
    }
  }

  private fun onItemLoaded(response: Item) {
    requireActivity().invalidateOptionsMenu()
    item = response
    bindContent()
  }

  private fun downloadFileAndRenderPdf() {
    val url = item.url ?: return offerExternalApp()
    fileDownloader.downloadFile(
        url,
        PDF_MIME_TYPE,
        object : FileDownloaderCallback {
          override fun onFailure(call: Call?, e: IOException?) {
            offerExternalApp()
          }

          override fun onSuccess(filePath: String?) {
            reloadUrl(PDF_LOADER_URL, filePath)
          }
        },
    )
  }

  private class ReadabilityCallback(webFragment: WebFragment) : ReadabilityClient.Callback {
    private val fragment = WeakReference(webFragment)

    override fun onResponse(content: String?) {
      fragment.get()?.takeIf { it.isAttached }?.onParsed(content)
    }
  }

  private class ItemResponseListener(webFragment: WebFragment) : ResponseListener<Item?> {
    private val fragment = WeakReference(webFragment)

    override fun onResponse(response: Item?) {
      if (response != null) {
        fragment.get()?.takeIf { it.isAttached }?.onItemLoaded(response)
      }
    }

    override fun onError(errorMessage: String?) {
      // do nothing
    }
  }

  /** Exposes a downloaded PDF to the bundled pdf.js viewer in chunks. */
  private class PdfAndroidJavascriptBridge(filePath: String, private val callback: Callbacks) {
    private val file = File(filePath)
    private val handler = Handler(Looper.getMainLooper())
    private var randomAccessFile: RandomAccessFile? = null

    @JavascriptInterface
    fun getChunk(begin: Long, end: Long): String =
        try {
          val raf = randomAccessFile ?: RandomAccessFile(file, "r").also { randomAccessFile = it }
          val data = ByteArray((end - begin).toInt())
          raf.seek(begin)
          raf.read(data)
          Base64.encodeToString(data, Base64.DEFAULT)
        } catch (e: IOException) {
          Log.e("Exception", e.toString())
          ""
        }

    @JavascriptInterface fun getSize(): Long = file.length()

    @JavascriptInterface
    fun onLoad() {
      handler.post { callback.onLoad() }
    }

    @JavascriptInterface
    fun onFailure() {
      handler.post { callback.onFailure() }
    }

    fun cleanUp() {
      try {
        randomAccessFile?.close()
      } catch (e: IOException) {
        Log.e("Exception", e.toString())
      }
    }

    protected fun finalize() {
      cleanUp()
    }

    interface Callbacks {
      fun onFailure()

      fun onLoad()
    }
  }

  companion object {
    @JvmField val EXTRA_ITEM: String = WebFragment::class.java.name + ".EXTRA_ITEM"
    @JvmField val ACTION_FULLSCREEN: String = WebFragment::class.java.name + ".ACTION_FULLSCREEN"
    @JvmField val EXTRA_FULLSCREEN: String = WebFragment::class.java.name + ".EXTRA_FULLSCREEN"
    private const val STATE_EMPTY = "state:empty"
    private const val STATE_READABILITY = "state:readability"
    private const val STATE_FULLSCREEN = "state:fullscreen"
    private const val STATE_CONTENT = "state:content"
    private const val DEFAULT_PROGRESS = 20
    private const val PDF_LOADER_URL = "file:///android_asset/pdf/index.html"
    private const val PDF_MIME_TYPE = "application/pdf"
    private const val PDF_BRIDGE_NAME = "PdfAndroidJavascriptBridge"

    private fun isHackerNewsUrl(item: WebItem): Boolean =
        !item.url.isNullOrEmpty() &&
            item.url == String.format(HackerNewsClient.WEB_ITEM_PATH, item.id)

    private fun wrapHtml(context: Context, html: String?): String =
        context.getString(
            R.string.html,
            getReadabilityTypeface(context),
            toHtmlPx(context, resolvePreferredReadabilityTextSize(context)),
            AppUtils.toHtmlColor(context, android.R.attr.textColorPrimary),
            AppUtils.toHtmlColor(context, android.R.attr.textColorLink),
            if (html.isNullOrEmpty()) context.getString(R.string.empty_text) else html,
            toHtmlPx(context, context.resources.getDimension(R.dimen.activity_vertical_margin)),
            toHtmlPx(context, context.resources.getDimension(R.dimen.activity_horizontal_margin)),
            getReadabilityLineHeight(context),
        )

    private fun toHtmlPx(context: Context, @StyleRes textStyleAttr: Int): Float =
        toHtmlPx(context, AppUtils.getDimension(context, textStyleAttr, R.attr.contentTextSize))

    private fun toHtmlPx(context: Context, dimen: Float): Float =
        dimen / context.resources.displayMetrics.density
  }
}
