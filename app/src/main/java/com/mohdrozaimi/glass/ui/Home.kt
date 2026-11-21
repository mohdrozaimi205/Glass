package com.mohdrozaimi.glass.ui

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdrozaimi.glass.data.AppEntry
import com.mohdrozaimi.glass.data.AppPrefs
import com.mohdrozaimi.glass.data.AppsRepository
import com.mohdrozaimi.glass.data.HomeItem
import com.mohdrozaimi.glass.data.PrayerZones
import com.mohdrozaimi.glass.data.ThemeId
import com.mohdrozaimi.glass.data.WidgetKind
import com.mohdrozaimi.glass.data.searchUrl
import com.mohdrozaimi.glass.glass.Backdrop
import com.mohdrozaimi.glass.glass.GlassCard
import com.mohdrozaimi.glass.glass.LocalBackdrop
import com.mohdrozaimi.glass.glass.LocalHomeVisit
import com.mohdrozaimi.glass.glass.LocalMotion
import com.mohdrozaimi.glass.glass.LocalWaveTime
import com.mohdrozaimi.glass.glass.TWO_PI
import com.mohdrozaimi.glass.glass.drawPastelWaves
import com.mohdrozaimi.glass.glass.entrance
import com.mohdrozaimi.glass.glass.PerfMode
import com.mohdrozaimi.glass.glass.drawBackdrop
import com.mohdrozaimi.glass.notif.BadgeStore
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.roundToInt

sealed interface DrawerMode {
    data object Normal : DrawerMode
    data object AddToHome : DrawerMode
    data class Replace(val id: String) : DrawerMode
}

sealed interface Overlay {
    data object None : Overlay
    data class Drawer(val mode: DrawerMode = DrawerMode.Normal) : Overlay
    data object Settings : Overlay
    data class ItemMenu(val id: String) : Overlay
    data object WidgetPicker : Overlay
    data object ZonePicker : Overlay
    data class Searching(val query: String, val url: String) : Overlay
}

@Composable
fun GlassRoot(vm: HomeViewModel, widgets: WidgetController) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { screen = it.toSize() },
    ) {
        val p = prefs ?: return@Box
        val lite = PerfMode.resolve(p.lite, vm.autoLite)
        val img = wallpaper
        val bd = if (img != null && screen.width > 0f) Backdrop(img, screen, lite) else null
        val waveTime = if (p.motion.wavesOn) {
            val tr = rememberInfiniteTransition(label = "waves")
            tr.animateFloat(
                0f,
                TWO_PI,
                infiniteRepeatable(tween(p.motion.ms(18000), easing = LinearEasing)),
                label = "waveTime",
            )
        } else {
            null
        }
        if (bd != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawBackdrop(bd.copy(dim = 0.04f), Offset.Zero)
                if (waveTime != null) drawPastelWaves(waveTime.value, bd.screen, Offset.Zero)
            }
        }
        CompositionLocalProvider(
            LocalBackdrop provides bd,
            LocalMotion provides p.motion,
            LocalWaveTime provides waveTime,
        ) {
            HomeScreen(vm, widgets, p)
        }
    }
}

fun styleFor(theme: ThemeId) = when (theme) {
    ThemeId.LIQUID -> WidgetStyle(Color(0xFFF5C518), FontWeight.Light, "Liquid Glass")
    ThemeId.HONOR -> WidgetStyle(Color(0xFF7FE0C9), FontWeight.SemiBold, "Honor")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel, widgets: WidgetController, p: AppPrefs) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val badges by BadgeStore.counts.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val prayer by vm.prayer.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val map by vm.map.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()

    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var edit by remember { mutableStateOf(false) }
    var visit by remember { mutableIntStateOf(0) }
    LifecycleStartEffect(Unit) {
        visit++
        onStopOrDispose { }
    }

    val now by produceState(LocalDateTime.now()) {
        while (true) {
            val wait = (60 - LocalDateTime.now().second) * 1000L
            delay(wait.coerceAtLeast(500L))
            value = LocalDateTime.now()
        }
    }

    val style = styleFor(p.theme)
    val items = remember(p) { vm.homeItems(p) }
    val appMap = remember(apps) { apps.associateBy { it.pkg } }
    val data = WidgetData(
        now = now,
        is24h = DateFormat.is24HourFormat(context),
        weather = weather,
        prayer = prayer,
        zoneName = PrayerZones.name(p.zone),
        events = events,
        map = map,
        hasLocation = location != null,
    )

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshAll(force = true)
    }
    val askPerms = {
        permLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CALENDAR))
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setWallpaperUri(uri)
    }

    LaunchedEffect(p.firstRun) {
        if (p.firstRun) {
            askPerms()
            vm.firstRunDone()
        }
    }
    LaunchedEffect(Unit) {
        vm.homeEvents.collect {
            overlay = Overlay.None
            edit = false
        }
    }
    BackHandler(enabled = true) {
        when {
            overlay == Overlay.ZonePicker -> overlay = Overlay.Settings
            overlay is Overlay.Searching -> overlay = Overlay.None
            overlay != Overlay.None -> overlay = Overlay.None
            edit -> edit = false
        }
    }

    val wiggle = rememberInfiniteTransition(label = "wiggle")
    val angle by wiggle.animateFloat(-1.3f, 1.3f, infiniteRepeatable(tween(150), RepeatMode.Reverse), label = "angle")

    CompositionLocalProvider(LocalHomeVisit provides visit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp),
        ) {
            if (edit) {
                EditBar(
                    accent = style.accent,
                    onAddApp = { overlay = Overlay.Drawer(DrawerMode.AddToHome) },
                    onAddWidget = { overlay = Overlay.WidgetPicker },
                    onSettings = { overlay = Overlay.Settings },
                    onDone = { edit = false },
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }

            HomeGridView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                items = items,
                edit = edit,
                wiggle = { angle },
                onLongPressEmpty = { edit = true },
                onSwipeUp = { if (!edit) overlay = Overlay.Drawer() },
                onMove = { id, x, y -> vm.moveItem(id, x, y) },
                onTap = { id -> overlay = Overlay.ItemMenu(id) },
                onRemove = { id ->
                    val it = items.firstOrNull { i -> i.id == id }
                    if (it is HomeItem.SystemWidget) widgets.delete(it.appWidgetId)
                    vm.removeItem(id)
                },
            ) { item ->
                when (item) {
                    is HomeItem.AppItem -> {
                        val app = appMap[item.pkg]
                        Box(
                            Modifier
                                .fillMaxSize()
                                .combinedClickable(
                                    onClick = { if (!AppsRepository.launch(context, item.pkg)) vm.toast("App tidak dijumpai") },
                                    onLongClick = { edit = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            val big = minOf(item.w, item.h)
                            AppIconView(app, badges[item.pkg] ?: 0, (54 * big).dp.coerceAtMost(120.dp), showLabel = true, accent = style.accent)
                        }
                    }
                    is HomeItem.GlassWidget -> {
                        val click: (() -> Unit)? = when (item.kind) {
                            WidgetKind.CALENDAR -> { { Intents.openCalendar(context) } }
                            WidgetKind.WEATHER -> { { searchUrl("cuaca")?.let { Intents.openUrl(context, it) } } }
                            WidgetKind.MAP -> map?.let { m -> { Intents.openMaps(context, m.lat, m.lon) } }
                            WidgetKind.CLOCK -> { { Intents.openAlarms(context) } }
                            else -> null
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxSize(),
                            onClick = click,
                            onLongClick = { edit = true },
                        ) {
                            when (item.kind) {
                                WidgetKind.CLOCK -> ClockWidget(data, style, item.w, item.h)
                                WidgetKind.CALENDAR -> CalendarWidget(data, style, item.w, item.h, onAdd = { Intents.addEvent(context) }, onGrant = askPerms)
                                WidgetKind.WEATHER -> WeatherWidget(data, style, item.w, item.h, onGrant = askPerms)
                                WidgetKind.MAP -> MapWidget(data, style, onGrant = askPerms)
                                WidgetKind.SEARCH -> SearchWidget(style, item.w) { q ->
                                    val url = searchUrl(q)
                                    if (url != null) {
                                        if (p.motion.searchingOn) overlay = Overlay.Searching(q.trim(), url) else Intents.openUrl(context, url)
                                    }
                                }
                                WidgetKind.PRAYER -> PrayerWidget(data, style, item.w, item.h)
                                WidgetKind.GREETING -> GreetingWidget(data, style, item.w, item.h)
                            }
                        }
                    }
                    is HomeItem.SystemWidget -> {
                        GlassCard(modifier = Modifier.fillMaxSize(), cornerRadius = 24.dp) {
                            SystemWidgetView(item, widgets)
                        }
                    }
                }
            }

            Dock(
                modifier = Modifier.entrance(items.size),
                p = p,
                apps = appMap,
                badges = badges,
                style = style,
                edit = edit,
                onLaunch = { pkg -> if (!AppsRepository.launch(context, pkg)) vm.toast("App tidak dijumpai") },
                onRemove = { pkg -> vm.removeFromDock(pkg) },
                onOpenDrawer = { overlay = Overlay.Drawer() },
                onLongPress = { edit = true },
            )
            Spacer(Modifier.height(8.dp))
        }

        // ---------------- lapisan atas ----------------
        when (val o = overlay) {
            is Overlay.Drawer -> DrawerOverlay(
                mode = o.mode,
                apps = apps,
                badges = badges,
                dock = p.dock,
                accent = style.accent,
                onPick = { pkg ->
                    when (val m = o.mode) {
                        DrawerMode.Normal -> if (!AppsRepository.launch(context, pkg)) vm.toast("App tidak dijumpai")
                        DrawerMode.AddToHome -> vm.addApp(pkg)
                        is DrawerMode.Replace -> vm.replaceApp(m.id, pkg)
                    }
                    overlay = Overlay.None
                },
                onAddHome = { pkg -> vm.addApp(pkg) },
                onAddDock = { pkg -> vm.addToDock(pkg) },
                onRemoveDock = { pkg -> vm.removeFromDock(pkg) },
                onClose = { overlay = Overlay.None },
            )
            is Overlay.ItemMenu -> {
                val item = items.firstOrNull { it.id == o.id }
                if (item == null) {
                    LaunchedEffect(o) { overlay = Overlay.None }
                } else {
                    ItemMenuSheet(
                        item = item,
                        items = items,
                        appLabel = (item as? HomeItem.AppItem)?.let { appMap[it.pkg]?.label },
                        accent = style.accent,
                        onResize = { w, h -> vm.resizeItem(item.id, w, h) },
                        onReplace = { overlay = Overlay.Drawer(DrawerMode.Replace(item.id)) },
                        onRemove = {
                            if (item is HomeItem.SystemWidget) widgets.delete(item.appWidgetId)
                            vm.removeItem(item.id)
                            overlay = Overlay.None
                        },
                        onClose = { overlay = Overlay.None },
                    )
                }
            }
            Overlay.WidgetPicker -> WidgetPickerOverlay(
                widgets = widgets,
                accent = style.accent,
                onGlass = { kind -> if (vm.addGlassWidget(kind)) overlay = Overlay.None },
                onSystem = { info ->
                    if (!vm.hasRoomForWidget()) {
                        vm.toast("Tiada ruang kosong")
                    } else {
                        widgets.begin(info) { id, provider -> vm.addSystemWidget(id, provider) { widgets.delete(id) } }
                        overlay = Overlay.None
                    }
                },
                onClose = { overlay = Overlay.None },
            )
            Overlay.Settings -> SettingsOverlay(
                p = p,
                autoLite = vm.autoLite,
                accent = style.accent,
                onTheme = vm::setTheme,
                onPreset = vm::setPresetWallpaper,
                onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onZone = { overlay = Overlay.ZonePicker },
                onLite = vm::setLite,
                onMotion = vm::setMotion,
                onPerms = askPerms,
                onNotif = { Intents.notificationAccess(context) },
                onDefault = { Intents.homeSettings(context) },
                onReset = {
                    vm.resetLayout()
                    vm.toast("Susun atur dikembalikan")
                },
                onClose = { overlay = Overlay.None },
            )
            Overlay.ZonePicker -> ZonePickerOverlay(
                current = p.zone,
                accent = style.accent,
                onPick = { z ->
                    vm.setZone(z)
                    overlay = Overlay.Settings
                },
                onClose = { overlay = Overlay.Settings },
            )
            is Overlay.Searching -> SearchingOverlay(o.query) {
                Intents.openUrl(context, o.url)
                overlay = Overlay.None
            }
            Overlay.None -> Unit
        }
    }
    }
}

@Composable
private fun EditBar(accent: Color, onAddApp: () -> Unit, onAddWidget: () -> Unit, onSettings: () -> Unit, onDone: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp).height(50.dp),
        cornerRadius = 25.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton("+ App", Color.White, onAddApp)
            BarButton("+ Widget", Color.White, onAddWidget)
            BarButton("Tetapan", Color.White, onSettings)
            BarButton("Selesai", accent, onDone)
        }
    }
}

@Composable
private fun BarButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp),
    ) { GText(text, 14.sp, weight = FontWeight.SemiBold, color = color) }
}

@Composable
private fun HomeGridView(
    modifier: Modifier,
    items: List<HomeItem>,
    edit: Boolean,
    wiggle: () -> Float,
    onLongPressEmpty: () -> Unit,
    onSwipeUp: () -> Unit,
    onMove: (String, Int, Int) -> Unit,
    onTap: (String) -> Unit,
    onRemove: (String) -> Unit,
    content: @Composable (HomeItem) -> Unit,
) {
    BoxWithConstraints(
        modifier
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPressEmpty() }) }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) onSwipeUp() },
                ) { _, dy -> total += dy }
            },
    ) {
        val cellW = maxWidth / com.mohdrozaimi.glass.data.HomeGrid.COLS
        val cellH = maxHeight / com.mohdrozaimi.glass.data.HomeGrid.ROWS
        val order = remember(items) {
            items.sortedWith(compareBy({ it.y }, { it.x })).withIndex().associate { it.value.id to it.index }
        }
        for (item in items) {
            androidx.compose.runtime.key(item.id) {
                GridCell(item, order[item.id] ?: 0, cellW, cellH, edit, wiggle, onMove, onTap, onRemove, content)
            }
        }
    }
}

@Composable
private fun GridCell(
    item: HomeItem,
    order: Int,
    cellW: Dp,
    cellH: Dp,
    edit: Boolean,
    wiggle: () -> Float,
    onMove: (String, Int, Int) -> Unit,
    onTap: (String) -> Unit,
    onRemove: (String) -> Unit,
    content: @Composable (HomeItem) -> Unit,
) {
    val density = LocalDensity.current
    val cw = with(density) { cellW.toPx() }
    val ch = with(density) { cellH.toPx() }
    var drag by remember(item.id, item.x, item.y) { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        Modifier
            .offset { IntOffset((item.x * cw + drag.x).roundToInt(), (item.y * ch + drag.y).roundToInt()) }
            .size(cellW * item.w, cellH * item.h)
            .entrance(order)
            .zIndex(if (dragging) 2f else 0f)
            .graphicsLayer {
                rotationZ = if (edit && !dragging) wiggle() else 0f
                val s = if (dragging) 1.05f else 1f
                scaleX = s
                scaleY = s
            }
            .padding(5.dp),
    ) {
        content(item)
        if (edit) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(item.id, item.x, item.y) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                val nx = ((item.x * cw + drag.x) / cw).roundToInt()
                                val ny = ((item.y * ch + drag.y) / ch).roundToInt()
                                dragging = false
                                drag = Offset.Zero
                                if (nx != item.x || ny != item.y) onMove(item.id, nx, ny)
                            },
                            onDragCancel = {
                                dragging = false
                                drag = Offset.Zero
                            },
                        ) { change, d ->
                            change.consume()
                            drag += d
                        }
                    }
                    .pointerInput(item.id) { detectTapGestures(onTap = { onTap(item.id) }) },
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset((-6).dp, (-6).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5484D))
                    .clickable { onRemove(item.id) },
                contentAlignment = Alignment.Center,
            ) { Text("−", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    modifier: Modifier,
    p: AppPrefs,
    apps: Map<String, AppEntry>,
    badges: Map<String, Int>,
    style: WidgetStyle,
    edit: Boolean,
    onLaunch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onLongPress: () -> Unit,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().height(86.dp),
        cornerRadius = 34.dp,
        onLongClick = onLongPress,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (p.dock.isEmpty()) {
                GText("Tekan lama app di menu → Tambah ke dok", 12.sp, alpha = 0.75f, modifier = Modifier.weight(1f))
            }
            for (pkg in p.dock) {
                Box {
                    Box(Modifier.combinedClickable(onClick = { onLaunch(pkg) }, onLongClick = onLongPress)) {
                        AppIconView(apps[pkg], badges[pkg] ?: 0, 50.dp, showLabel = false, accent = style.accent)
                    }
                    if (edit) {
                        Box(
                            Modifier.align(Alignment.TopStart).offset((-6).dp, (-6).dp).size(20.dp).clip(CircleShape)
                                .background(Color(0xFFE5484D)).clickable { onRemove(pkg) },
                            contentAlignment = Alignment.Center,
                        ) { Text("−", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(style.accent.copy(alpha = 0.92f)).clickable { onOpenDrawer() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (p.theme == ThemeId.HONOR) Icons.Filled.Search else Icons.Filled.Menu,
                    contentDescription = "Semua apps",
                    tint = Color.Black,
                )
            }
        }
    }
}
