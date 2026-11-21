package com.mohdrozaimi.glass.ui

import android.app.ActivityManager
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohdrozaimi.glass.data.AppEntry
import com.mohdrozaimi.glass.data.AppPrefs
import com.mohdrozaimi.glass.data.AppsRepository
import com.mohdrozaimi.glass.data.CalEvent
import com.mohdrozaimi.glass.data.CalendarRepo
import com.mohdrozaimi.glass.data.DockList
import com.mohdrozaimi.glass.data.HomeGrid
import com.mohdrozaimi.glass.data.HomeItem
import com.mohdrozaimi.glass.data.LocationRepo
import com.mohdrozaimi.glass.data.MapRepo
import com.mohdrozaimi.glass.data.PrayerDay
import com.mohdrozaimi.glass.data.PrayerRepo
import com.mohdrozaimi.glass.data.Settings
import com.mohdrozaimi.glass.data.ThemeId
import com.mohdrozaimi.glass.data.Wallpapers
import com.mohdrozaimi.glass.data.Weather
import com.mohdrozaimi.glass.data.WeatherRepo
import com.mohdrozaimi.glass.data.WidgetKind
import com.mohdrozaimi.glass.glass.PerfMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val r: com.mohdrozaimi.glass.data.ReleaseInfo) : UpdateState
    data class Downloading(val r: com.mohdrozaimi.glass.data.ReleaseInfo, val progress: Float) : UpdateState
    data class Ready(val r: com.mohdrozaimi.glass.data.ReleaseInfo, val file: java.io.File) : UpdateState
    data class Failed(val msg: String) : UpdateState
}

data class MapView(val image: ImageBitmap, val point: Pair<Float, Float>, val lat: Double, val lon: Double)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()
    val settings = Settings(app)
    private val weatherRepo = WeatherRepo(settings)
    private val prayerRepo = PrayerRepo(settings)

    val prefs: StateFlow<AppPrefs?> = settings.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    private val _prayer = MutableStateFlow<List<PrayerDay>>(emptyList())
    val prayer: StateFlow<List<PrayerDay>> = _prayer.asStateFlow()

    /** null = tiada kebenaran kalendar. */
    private val _events = MutableStateFlow<List<CalEvent>?>(emptyList())
    val events: StateFlow<List<CalEvent>?> = _events.asStateFlow()

    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location.asStateFlow()

    private val _map = MutableStateFlow<MapView?>(null)
    val map: StateFlow<MapView?> = _map.asStateFlow()

    private val _wallpaper = MutableStateFlow<ImageBitmap?>(null)
    val wallpaper: StateFlow<ImageBitmap?> = _wallpaper.asStateFlow()

    private val _homeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeEvents = _homeEvents.asSharedFlow()

    val autoLite: Boolean = run {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        PerfMode.shouldLite(mi.totalMem, am.isLowRamDevice)
    }

    private var lastWeatherAt = 0L

    init {
        viewModelScope.launch {
            prefs.filterNotNull().map { it.wallpaper }.distinctUntilChanged().collect { loadWallpaper(it) }
        }
        viewModelScope.launch {
            prefs.filterNotNull().map { it.zone }.distinctUntilChanged().collect { z ->
                _prayer.value = prayerRepo.get(z)
            }
        }
        viewModelScope.launch { _weather.value = weatherRepo.cached() }
        refreshApps()
    }

    fun onHomePressed() { _homeEvents.tryEmit(Unit) }

    // ---------- muat semula data ----------
    fun refreshApps() {
        viewModelScope.launch { _apps.value = withContext(Dispatchers.IO) { AppsRepository.load(ctx) } }
    }

    fun refreshAll(force: Boolean = false) {
        refreshApps()
        viewModelScope.launch { _events.value = withContext(Dispatchers.IO) { CalendarRepo.upcoming(ctx) } }
        viewModelScope.launch {
            val p = prefs.value
            if (p != null) _prayer.value = prayerRepo.get(p.zone)
        }
        viewModelScope.launch {
            val loc = LocationRepo.get(ctx)
            _location.value = loc
            if (loc != null) {
                val now = System.currentTimeMillis()
                if (force || now - lastWeatherAt > 30 * 60_000L || _weather.value == null) {
                    weatherRepo.get(loc.first, loc.second)?.let {
                        _weather.value = it
                        lastWeatherAt = now
                    }
                }
                val m = _map.value
                val moved = m == null || Math.abs(m.lat - loc.first) > 0.01 || Math.abs(m.lon - loc.second) > 0.01
                if (moved) {
                    MapRepo.get(ctx, loc.first, loc.second)?.let { (bmp, pt) ->
                        _map.value = MapView(bmp.asImageBitmap(), pt, loc.first, loc.second)
                    }
                }
            }
        }
    }

    private suspend fun loadWallpaper(spec: String) {
        val dm = ctx.resources.displayMetrics
        val bmp: Bitmap = withContext(Dispatchers.Default) {
            Wallpapers.load(ctx, spec, dm.widthPixels, dm.heightPixels)
        }
        _wallpaper.value = bmp.asImageBitmap()
    }

    // ---------- tetapan ----------
    fun setTheme(t: ThemeId) = viewModelScope.launch { settings.setTheme(t) }
    fun setZone(z: String) = viewModelScope.launch { settings.setZone(z) }
    fun setLite(v: String) = viewModelScope.launch { settings.setLite(v) }
    fun setShowDock(v: Boolean) = viewModelScope.launch { settings.setShowDock(v) }
    fun setMotion(m: com.mohdrozaimi.glass.data.MotionPrefs) = viewModelScope.launch { settings.setMotion(m) }
    fun setPresetWallpaper(i: Int) = viewModelScope.launch { settings.setWallpaper("preset:$i") }
    fun firstRunDone() = viewModelScope.launch { settings.setFirstRunDone() }

    fun setWallpaperUri(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { Wallpapers.saveFromUri(ctx, uri) }
        if (ok) {
            settings.setWallpaper("file")
            loadWallpaper("file")
        } else {
            toast("Gambar tidak dapat dibuka")
        }
    }

    // ---------- home screen ----------
    fun homeItems(p: AppPrefs): List<HomeItem> =
        p.homeRaw(p.theme)?.let { HomeGrid.decode(it) } ?: HomeGrid.defaultLayout(p.theme)

    private fun editHome(transform: (List<HomeItem>) -> List<HomeItem>) {
        val p = prefs.value ?: return
        viewModelScope.launch { settings.updateHome(p.theme, transform) }
    }

    fun moveItem(id: String, x: Int, y: Int) = editHome { HomeGrid.move(it, id, x, y) }
    fun resizeItem(id: String, w: Int, h: Int) = editHome { HomeGrid.resize(it, id, w, h) }
    fun removeItem(id: String) = editHome { HomeGrid.remove(it, id) }
    fun replaceApp(id: String, pkg: String) = editHome { HomeGrid.replaceApp(it, id, pkg) }

    fun addApp(pkg: String) {
        val p = prefs.value ?: return
        val items = homeItems(p)
        if (HomeGrid.firstFree(items, 1, 1) == null) { toast("Home screen penuh"); return }
        editHome { cur ->
            val spot = HomeGrid.firstFree(cur, 1, 1) ?: return@editHome cur
            HomeGrid.add(cur, HomeItem.AppItem(newId(), spot.first, spot.second, 1, 1, pkg))
        }
        toast("Ditambah ke home")
    }

    /** Pulang false jika tiada ruang. */
    fun addGlassWidget(kind: WidgetKind): Boolean {
        val p = prefs.value ?: return false
        val (w, h) = fitSize(homeItems(p), HomeGrid.defaultSize(kind)) ?: run { toast("Tiada ruang kosong"); return false }
        editHome { cur ->
            val spot = HomeGrid.firstFree(cur, w, h) ?: return@editHome cur
            HomeGrid.add(cur, HomeItem.GlassWidget(newId(), spot.first, spot.second, w, h, kind))
        }
        return true
    }

    fun hasRoomForWidget(): Boolean {
        val p = prefs.value ?: return false
        return HomeGrid.firstFree(homeItems(p), 1, 1) != null
    }

    fun addSystemWidget(appWidgetId: Int, provider: String, onFail: () -> Unit) {
        val p = prefs.value ?: return
        val size = fitSize(homeItems(p), 4 to 2)
        if (size == null) { onFail(); toast("Tiada ruang kosong"); return }
        editHome { cur ->
            val spot = HomeGrid.firstFree(cur, size.first, size.second) ?: return@editHome cur
            HomeGrid.add(cur, HomeItem.SystemWidget(newId(), spot.first, spot.second, size.first, size.second, appWidgetId, provider))
        }
    }

    /** Cari saiz terbesar yang muat, bermula dengan [preferred]. */
    private fun fitSize(items: List<HomeItem>, preferred: Pair<Int, Int>): Pair<Int, Int>? {
        val order = listOf(preferred) + listOf(4 to 2, 2 to 2, 4 to 1, 2 to 1, 1 to 1)
        return order.firstOrNull { (w, h) -> HomeGrid.firstFree(items, w, h) != null }
    }

    fun resetLayout() {
        val p = prefs.value ?: return
        viewModelScope.launch { settings.setHome(p.theme, null) }
    }

    // ---------- dok ----------
    fun addToDock(pkg: String) {
        val p = prefs.value ?: return
        if (p.dock.size >= DockList.MAX && pkg !in p.dock) { toast("Dok penuh (maksimum ${DockList.MAX})"); return }
        viewModelScope.launch { settings.updateDock { DockList.add(it, pkg) } }
    }

    fun removeFromDock(pkg: String) = viewModelScope.launch { settings.updateDock { DockList.remove(it, pkg) } }

    // ---------------- kemas kini app ----------------
    private val updater = com.mohdrozaimi.glass.data.Updater(app)
    val currentVersion: String = updater.currentVersion()
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    fun checkUpdate() {
        if (_update.value is UpdateState.Checking || _update.value is UpdateState.Downloading) return
        _update.value = UpdateState.Checking
        viewModelScope.launch {
            _update.value = try {
                val r = updater.latest()
                when {
                    r == null -> UpdateState.Failed("Tiada fail APK dalam versi terbaru")
                    com.mohdrozaimi.glass.data.Update.isNewer(r.version, currentVersion) -> UpdateState.Available(r)
                    else -> UpdateState.UpToDate
                }
            } catch (e: Exception) {
                UpdateState.Failed("Tak dapat semak. Pastikan ada internet.")
            }
        }
    }

    fun downloadUpdate() {
        val r = when (val s = _update.value) {
            is UpdateState.Available -> s.r
            is UpdateState.Failed -> return checkUpdate()
            else -> return
        }
        _update.value = UpdateState.Downloading(r, 0f)
        viewModelScope.launch {
            _update.value = try {
                val f = updater.download(r) { p -> _update.value = UpdateState.Downloading(r, p) }
                installOrAsk(f)
                UpdateState.Ready(r, f)
            } catch (e: Exception) {
                UpdateState.Failed("Muat turun gagal. Cuba lagi.")
            }
        }
    }

    fun installUpdate() {
        val s = _update.value as? UpdateState.Ready ?: return
        installOrAsk(s.file)
    }

    private fun installOrAsk(f: java.io.File) {
        try {
            if (updater.canInstall()) {
                updater.install(f)
            } else {
                toast("Benarkan Glass memasang app, kemudian tekan Pasang")
                updater.openInstallPermission()
            }
        } catch (e: Exception) {
            toast("Tak dapat buka pemasangan")
        }
    }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    private fun newId() = UUID.randomUUID().toString().take(8)
}
