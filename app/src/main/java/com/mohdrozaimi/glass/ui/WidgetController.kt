package com.mohdrozaimi.glass.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent

/** Uruskan widget dari app lain (pilih, beri kebenaran, konfigurasi, buang). */
class WidgetController(private val activity: Activity) {
    val host = AppWidgetHost(activity.applicationContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)

    private class Pending(val id: Int, val info: AppWidgetProviderInfo, val onAdded: (Int, String) -> Unit)
    private var pending: Pending? = null

    fun providers(): List<AppWidgetProviderInfo> = try {
        manager.installedProviders.sortedBy { it.loadLabel(activity.packageManager).lowercase() }
    } catch (e: Exception) {
        emptyList()
    }

    fun info(id: Int): AppWidgetProviderInfo? = try { manager.getAppWidgetInfo(id) } catch (e: Exception) { null }

    fun begin(info: AppWidgetProviderInfo, onAdded: (appWidgetId: Int, provider: String) -> Unit) {
        val id = host.allocateAppWidgetId()
        pending = Pending(id, info, onAdded)
        if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configure()
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            @Suppress("DEPRECATION")
            activity.startActivityForResult(i, REQ_BIND)
        }
    }

    private fun configure() {
        val p = pending ?: return
        if (p.info.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, p.id, 0, REQ_CONFIG, null)
            } catch (e: Exception) {
                finish(true)
            }
        } else {
            finish(true)
        }
    }

    /** Panggil dari onActivityResult. Pulang true jika diuruskan. */
    fun onResult(requestCode: Int, ok: Boolean): Boolean {
        when (requestCode) {
            REQ_BIND -> if (ok) configure() else finish(false)
            REQ_CONFIG -> finish(ok)
            else -> return false
        }
        return true
    }

    private fun finish(ok: Boolean) {
        val p = pending ?: return
        pending = null
        if (ok) p.onAdded(p.id, p.info.provider.flattenToString()) else host.deleteAppWidgetId(p.id)
    }

    fun delete(id: Int) {
        try { host.deleteAppWidgetId(id) } catch (e: Exception) { }
    }

    companion object {
        const val HOST_ID = 1024
        const val REQ_BIND = 4101
        const val REQ_CONFIG = 4102
    }
}
