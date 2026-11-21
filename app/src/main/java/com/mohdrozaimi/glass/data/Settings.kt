package com.mohdrozaimi.glass.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.glassStore by preferencesDataStore(name = "glass")

data class AppPrefs(
    val theme: ThemeId,
    val wallpaper: String,
    val zone: String,
    val lite: String,
    val homeLiquid: String?,
    val homeHonor: String?,
    val dock: List<String>,
    val firstRun: Boolean,
) {
    fun homeRaw(t: ThemeId): String? = if (t == ThemeId.LIQUID) homeLiquid else homeHonor
}

class Settings(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val wallpaper = stringPreferencesKey("wallpaper")
        val zone = stringPreferencesKey("zone")
        val lite = stringPreferencesKey("lite")
        val homeLiquid = stringPreferencesKey("home_LIQUID")
        val homeHonor = stringPreferencesKey("home_HONOR")
        val dock = stringPreferencesKey("dock")
        val firstRun = booleanPreferencesKey("first_run")
    }

    val prefs: Flow<AppPrefs> = context.glassStore.data.map { p: Preferences ->
        AppPrefs(
            theme = ThemeId.from(p[K.theme]),
            wallpaper = p[K.wallpaper] ?: "preset:0",
            zone = p[K.zone] ?: "SGR01",
            lite = p[K.lite] ?: "auto",
            homeLiquid = p[K.homeLiquid],
            homeHonor = p[K.homeHonor],
            dock = DockList.decode(p[K.dock]),
            firstRun = p[K.firstRun] ?: true,
        )
    }

    suspend fun setTheme(t: ThemeId) { context.glassStore.edit { it[K.theme] = t.name } }
    suspend fun setWallpaper(spec: String) { context.glassStore.edit { it[K.wallpaper] = spec } }
    suspend fun setZone(z: String) { context.glassStore.edit { it[K.zone] = z } }
    suspend fun setLite(v: String) { context.glassStore.edit { it[K.lite] = v } }
    suspend fun setDock(list: List<String>) { context.glassStore.edit { it[K.dock] = DockList.encode(list) } }
    suspend fun setFirstRunDone() { context.glassStore.edit { it[K.firstRun] = false } }

    suspend fun setHome(t: ThemeId, encoded: String?) {
        val key = if (t == ThemeId.LIQUID) K.homeLiquid else K.homeHonor
        context.glassStore.edit { p ->
            if (encoded == null) {
                p.remove(key)
            } else {
                p[key] = encoded
            }
        }
    }

    /** Ubah susun atur home secara atomik (elak kemaskini hilang bila tekan cepat). */
    suspend fun updateHome(t: ThemeId, transform: (List<HomeItem>) -> List<HomeItem>): List<HomeItem> {
        val key = if (t == ThemeId.LIQUID) K.homeLiquid else K.homeHonor
        var result: List<HomeItem> = emptyList()
        context.glassStore.edit { p ->
            val cur = p[key]?.let { HomeGrid.decode(it) } ?: HomeGrid.defaultLayout(t)
            result = transform(cur)
            p[key] = HomeGrid.encode(result)
        }
        return result
    }

    suspend fun updateDock(transform: (List<String>) -> List<String>) {
        context.glassStore.edit { p -> p[K.dock] = DockList.encode(transform(DockList.decode(p[K.dock]))) }
    }

    suspend fun cache(key: String): String? = context.glassStore.data.first()[stringPreferencesKey("c_$key")]
    suspend fun putCache(key: String, value: String) {
        context.glassStore.edit { it[stringPreferencesKey("c_$key")] = value }
    }
}
