package com.growse.android.io.github.hidroh.materialistic.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.RemoteViews
import com.growse.android.io.github.hidroh.materialistic.ItemActivity
import com.growse.android.io.github.hidroh.materialistic.R
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WidgetHelperTest :
    StringSpec(
        {
          "item template flags are mutable on Android 12+ so fill-in intents merge" {
            val flags = WidgetHelper.itemTemplateFlags(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

            (flags and PendingIntent.FLAG_MUTABLE) shouldBe PendingIntent.FLAG_MUTABLE
            (flags and PendingIntent.FLAG_UPDATE_CURRENT) shouldBe PendingIntent.FLAG_UPDATE_CURRENT
            (flags and PendingIntent.FLAG_IMMUTABLE) shouldBe 0
          }

          "item template flags omit FLAG_MUTABLE before Android 12" {
            val flags = WidgetHelper.itemTemplateFlags(Build.VERSION_CODES.R)

            flags shouldBe PendingIntent.FLAG_UPDATE_CURRENT
          }

          "item template intent explicitly targets ItemActivity with ACTION_VIEW" {
            val context: Context = mock()
            var constructorArgs: List<Any?>? = null

            mockConstruction(Intent::class.java) { _, ctx -> constructorArgs = ctx.arguments() }
                .use {
                  val intent = WidgetHelper.createItemTemplateIntent(context)

                  constructorArgs shouldBe listOf(context, ItemActivity::class.java)
                  verify(intent).action = Intent.ACTION_VIEW
                }
          }

          "update registers an explicit mutable PendingIntent template on android.R.id.list" {
            val appWidgetId = 42
            val context: Context = mock()
            val prefs: SharedPreferences = mock()
            val appWidgetManager: AppWidgetManager = mock()
            val templatePendingIntent: PendingIntent = mock()
            val otherPendingIntent: PendingIntent = mock()
            var itemTemplateIntent: Intent? = null

            whenever(context.packageName)
                .thenReturn("com.growse.android.io.github.hidroh.materialistic")
            whenever(context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)))
                .thenReturn(prefs)
            whenever(context.getString(R.string.pref_widget_theme_value_dark)).thenReturn("dark")
            whenever(context.getString(R.string.pref_widget_theme_value_light)).thenReturn("light")
            whenever(context.getString(R.string.pref_widget_section_value_best)).thenReturn("best")
            whenever(context.getString(R.string.pref_widget_section_value_top)).thenReturn("top")
            whenever(context.getString(R.string.title_activity_new)).thenReturn("New")

            mockStatic(AppWidgetManager::class.java).use { appWidgetManagers ->
              appWidgetManagers
                  .`when`<AppWidgetManager> { AppWidgetManager.getInstance(context) }
                  .thenReturn(appWidgetManager)

              mockConstruction(RemoteViews::class.java).use { remoteViewsConstruction ->
                mockConstruction(Intent::class.java) { mockIntent, ctx ->
                      whenever(mockIntent.putExtra(any(), any<Int>())).thenReturn(mockIntent)
                      whenever(mockIntent.putExtra(any(), any<Boolean>())).thenReturn(mockIntent)
                      whenever(mockIntent.putExtra(any(), anyOrNull<String>()))
                          .thenReturn(mockIntent)
                      whenever(mockIntent.setPackage(any())).thenReturn(mockIntent)
                      if (ctx.arguments() == listOf(context, ItemActivity::class.java)) {
                        itemTemplateIntent = mockIntent
                      }
                    }
                    .use {
                      mockStatic(PendingIntent::class.java).use { pendingIntents ->
                        pendingIntents
                            .`when`<PendingIntent> {
                              PendingIntent.getActivity(any(), any(), any(), any())
                            }
                            .thenAnswer { invocation ->
                              val intentArg = invocation.getArgument<Intent>(2)
                              if (intentArg === itemTemplateIntent) templatePendingIntent
                              else otherPendingIntent
                            }
                        pendingIntents
                            .`when`<PendingIntent> {
                              PendingIntent.getBroadcast(any(), any(), any(), any())
                            }
                            .thenReturn(otherPendingIntent)

                        WidgetHelper(context).update(appWidgetId)

                        val expectedIntent = itemTemplateIntent!!
                        pendingIntents.verify {
                          PendingIntent.getActivity(
                              context,
                              appWidgetId,
                              expectedIntent,
                              WidgetHelper.itemTemplateFlags(),
                          )
                        }
                        val remoteViews = remoteViewsConstruction.constructed().single()
                        verify(remoteViews)
                            .setPendingIntentTemplate(android.R.id.list, templatePendingIntent)
                      }
                    }
              }
            }
          }
        },
    )
