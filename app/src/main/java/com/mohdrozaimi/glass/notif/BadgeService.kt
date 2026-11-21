package com.mohdrozaimi.glass.notif

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BadgeStore {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()
    fun set(v: Map<String, Int>) { _counts.value = v }
}

/** Baca notifikasi aktif untuk tunjuk tanda (lencana) pada ikon app. */
class BadgeService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() = BadgeStore.set(emptyMap())
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = try { activeNotifications } catch (e: Exception) { null } ?: return
        BadgeStore.set(countBadges(list.map { it.packageName to it.isOngoing }))
    }
}
