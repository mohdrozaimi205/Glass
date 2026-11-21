package com.mohdrozaimi.glass.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object Intents {
    private fun go(context: Context, i: Intent): Boolean = try {
        context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun openUrl(context: Context, url: String) = go(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun openCalendar(context: Context) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        go(context, Intent(Intent.ACTION_VIEW, uri))
    }

    fun addEvent(context: Context) = go(context, Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI))

    fun openMaps(context: Context, lat: Double, lon: Double) {
        if (!go(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon")))) {
            openUrl(context, "https://www.google.com/maps/@$lat,$lon,15z")
        }
    }

    fun openAlarms(context: Context) = go(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun appInfo(context: Context, pkg: String) =
        go(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(context: Context, pkg: String) = go(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun notificationAccess(context: Context) = go(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun homeSettings(context: Context) = go(context, Intent(Settings.ACTION_HOME_SETTINGS))

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
