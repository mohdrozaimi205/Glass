package com.mohdrozaimi.glass.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.mohdrozaimi.glass.data.AppEntry
import com.mohdrozaimi.glass.data.AppPrefs
import com.mohdrozaimi.glass.data.HomeGrid
import com.mohdrozaimi.glass.data.HomeItem
import com.mohdrozaimi.glass.data.Motion
import com.mohdrozaimi.glass.data.MotionPrefs
import com.mohdrozaimi.glass.data.PrayerZones
import com.mohdrozaimi.glass.data.ThemeId
import com.mohdrozaimi.glass.data.Wallpapers
import com.mohdrozaimi.glass.data.WidgetKind
import com.mohdrozaimi.glass.glass.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SheetTint = Color.Black.copy(alpha = 0.28f)

/** Latar penuh skrin berkaca untuk menu besar. */
@Composable
private fun FullGlass(content: @Composable () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxSize(),
        cornerRadius = 0.dp,
        blur = 40.dp,
        tint = SheetTint,
        refraction = 0.15f,
        pressEffect = false,
        onClick = {},
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) { content() }
    }
}

@Composable
private fun Header(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GText(title, 22.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White) }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(18.dp))
    GText(text, 13.sp, weight = FontWeight.SemiBold, alpha = 0.7f)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Chip(text: String, selected: Boolean, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Color.White.copy(alpha = if (enabled) 0.14f else 0.05f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            color = if (selected) Color.Black else Color.White.copy(alpha = if (enabled) 1f else 0.35f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RowButton(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        GText(title, 15.sp, weight = FontWeight.Medium)
        if (subtitle != null) GText(subtitle, 12.sp, alpha = 0.65f, maxLines = 2)
    }
    Spacer(Modifier.height(8.dp))
}

// ======================================================= MENU SEMUA APPS
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerOverlay(
    mode: DrawerMode,
    apps: List<AppEntry>,
    badges: Map<String, Int>,
    dock: List<String>,
    accent: Color,
    onPick: (String) -> Unit,
    onAddHome: (String) -> Unit,
    onAddDock: (String) -> Unit,
    onRemoveDock: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var q by remember { mutableStateOf("") }
    val shown = remember(apps, q) {
        val t = q.trim()
        if (t.isEmpty()) apps else apps.filter { it.label.contains(t, ignoreCase = true) }
    }
    FullGlass {
        Header(
            when (mode) {
                DrawerMode.Normal -> "Semua Apps"
                DrawerMode.AddToHome -> "Pilih app untuk home"
                is DrawerMode.Replace -> "Tukar kepada app..."
            },
            onClose,
        )
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) GText("Cari app...", 15.sp, alpha = 0.6f)
                BasicTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shown, key = { it.pkg }) { app ->
                var menu by remember { mutableStateOf(false) }
                Box(
                    Modifier.combinedClickable(
                        onClick = { onPick(app.pkg) },
                        onLongClick = { if (mode == DrawerMode.Normal) menu = true },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconView(app, badges[app.pkg] ?: 0, 54.dp, showLabel = true, accent = accent)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Tambah ke home") }, onClick = { menu = false; onAddHome(app.pkg) })
                        if (app.pkg in dock) {
                            DropdownMenuItem(text = { Text("Buang dari dok") }, onClick = { menu = false; onRemoveDock(app.pkg) })
                        } else {
                            DropdownMenuItem(text = { Text("Tambah ke dok") }, onClick = { menu = false; onAddDock(app.pkg) })
                        }
                        DropdownMenuItem(text = { Text("Maklumat app") }, onClick = { menu = false; Intents.appInfo(context, app.pkg) })
                        DropdownMenuItem(text = { Text("Nyahpasang") }, onClick = { menu = false; Intents.uninstall(context, app.pkg) })
                    }
                }
            }
        }
    }
}

// ======================================================= MENU ITEM (mod edit)
@Composable
fun ItemMenuSheet(
    item: HomeItem,
    items: List<HomeItem>,
    appLabel: String?,
    accent: Color,
    onResize: (Int, Int) -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val fits = remember(items, item) { HomeGrid.sizesThatFit(items, item.id).toSet() }
    val title = when (item) {
        is HomeItem.AppItem -> appLabel ?: "App"
        is HomeItem.GlassWidget -> "Widget ${item.kind.label}"
        is HomeItem.SystemWidget -> "Widget app"
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
    ) {
        GlassCard(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(12.dp),
            cornerRadius = 30.dp,
            blur = 32.dp,
            tint = SheetTint,
            refraction = 0.25f,
            pressEffect = false,
            onClick = {},
        ) {
            Column(Modifier.padding(20.dp)) {
                GText(title, 18.sp, weight = FontWeight.SemiBold)
                Section("Saiz")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((w, h) in HomeGrid.SIZES) {
                        val current = item.w == w && item.h == h
                        Chip("${w}x$h", current, accent, enabled = current || (w to h) in fits) { onResize(w, h) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (item is HomeItem.AppItem) RowButton("Tukar app", "Pilih app lain untuk tempat ini", onReplace)
                RowButton("Buang dari home", null, onRemove)
                Spacer(Modifier.height(4.dp))
                GText("Seret item untuk alih kedudukan.", 12.sp, alpha = 0.6f)
            }
        }
    }
}

// ======================================================= PEMILIH WIDGET
@Composable
fun WidgetPickerOverlay(
    widgets: WidgetController,
    accent: Color,
    onGlass: (WidgetKind) -> Unit,
    onSystem: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val providers by produceState<List<AppWidgetProviderInfo>>(emptyList()) {
        value = withContext(Dispatchers.IO) { widgets.providers() }
    }
    FullGlass {
        Header("Tambah Widget", onClose)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                GText("Widget Glass", 13.sp, weight = FontWeight.SemiBold, alpha = 0.7f)
                Spacer(Modifier.height(8.dp))
                val kinds = WidgetKind.entries
                for (row in kinds.chunked(2)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (k in row) {
                            GlassCard(
                                modifier = Modifier.weight(1f).height(64.dp),
                                cornerRadius = 20.dp,
                                onClick = { onGlass(k) },
                            ) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    GText(kindEmoji(k), 22.sp)
                                    Spacer(Modifier.width(10.dp))
                                    GText(k.label, 15.sp, weight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(10.dp))
                GText("Widget App", 13.sp, weight = FontWeight.SemiBold, alpha = 0.7f)
                Spacer(Modifier.height(8.dp))
                if (providers.isEmpty()) GText("Memuatkan...", 13.sp, alpha = 0.6f)
            }
            items(providers, key = { it.provider.flattenToString() }) { info ->
                val pm = context.packageManager
                val label = remember(info) { info.loadLabel(pm) }
                val appName = remember(info) {
                    try { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() } catch (e: Exception) { "" }
                }
                val preview by produceState<ImageBitmap?>(null, info) {
                    value = withContext(Dispatchers.IO) {
                        try {
                            val dpi = context.resources.displayMetrics.densityDpi
                            val d = info.loadPreviewImage(context, dpi) ?: info.loadIcon(context, dpi)
                            d?.toBitmap(d.intrinsicWidth.coerceIn(1, 480), d.intrinsicHeight.coerceIn(1, 480))?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f)).clickable { onSystem(info) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(width = 84.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f))) {
                        preview?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        GText(label, 14.sp, weight = FontWeight.Medium)
                        GText(appName, 12.sp, alpha = 0.65f)
                    }
                    GText("+", 22.sp, color = accent)
                }
            }
        }
    }
}

private fun kindEmoji(k: WidgetKind) = when (k) {
    WidgetKind.CLOCK -> "🕒"
    WidgetKind.CALENDAR -> "📅"
    WidgetKind.WEATHER -> "⛅"
    WidgetKind.MAP -> "🗺️"
    WidgetKind.SEARCH -> "🔍"
    WidgetKind.PRAYER -> "🕌"
    WidgetKind.GREETING -> "👋"
}

// ======================================================= TETAPAN
@Composable
fun SettingsOverlay(
    p: AppPrefs,
    autoLite: Boolean,
    accent: Color,
    onTheme: (ThemeId) -> Unit,
    onPreset: (Int) -> Unit,
    onPickPhoto: () -> Unit,
    onZone: () -> Unit,
    onLite: (String) -> Unit,
    onMotion: (MotionPrefs) -> Unit,
    onPerms: () -> Unit,
    onNotif: () -> Unit,
    onDefault: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val thumbs by produceState<List<ImageBitmap>>(emptyList()) {
        value = withContext(Dispatchers.Default) {
            Wallpapers.presetNames.indices.map { Wallpapers.renderPreset(it, 180, 360).asImageBitmap() }
        }
    }
    FullGlass {
        Header("Tetapan Glass", onClose)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Section("Tema")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (t in ThemeId.entries) {
                    val st = styleFor(t)
                    val sel = p.theme == t
                    GlassCard(
                        modifier = Modifier.weight(1f).height(84.dp)
                            .then(if (sel) Modifier.border(2.dp, accent, RoundedCornerShape(22.dp)) else Modifier),
                        cornerRadius = 22.dp,
                        onClick = { onTheme(t) },
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(st.accent))
                            Spacer(Modifier.height(6.dp))
                            GText(st.name, 16.sp, weight = FontWeight.SemiBold)
                            GText(if (sel) "Sedang digunakan" else "Ketik untuk guna", 11.sp, alpha = 0.65f)
                        }
                    }
                }
            }

            Section("Wallpaper")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                thumbs.forEachIndexed { i, img ->
                    val sel = p.wallpaper == "preset:$i"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            img,
                            contentDescription = Wallpapers.presetNames[i],
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 72.dp, height = 128.dp).clip(RoundedCornerShape(14.dp))
                                .border(if (sel) 2.dp else 1.dp, if (sel) accent else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .clickable { onPreset(i) },
                        )
                        Spacer(Modifier.height(4.dp))
                        GText(Wallpapers.presetNames[i], 10.sp, alpha = 0.8f)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(width = 72.dp, height = 128.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(if (p.wallpaper == "file") 2.dp else 1.dp, if (p.wallpaper == "file") accent else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable { onPickPhoto() },
                        contentAlignment = Alignment.Center,
                    ) { GText("📷", 26.sp) }
                    Spacer(Modifier.height(4.dp))
                    GText("Gambar sendiri", 10.sp, alpha = 0.8f)
                }
            }

            Section("Waktu solat")
            RowButton("Zon: ${p.zone}", PrayerZones.name(p.zone), onZone)

            Section("Mod Jimat (kesan kaca ringan)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Auto", p.lite == "auto", accent) { onLite("auto") }
                Chip("Hidup", p.lite == "on", accent) { onLite("on") }
                Chip("Mati", p.lite == "off", accent) { onLite("off") }
            }
            Spacer(Modifier.height(6.dp))
            GText(
                "Auto untuk telefon ini: " + if (autoLite) "Hidup (kesan dikurangkan)" else "Mati (liquid glass penuh)",
                12.sp, alpha = 0.65f, maxLines = 2,
            )

            MotionSettings(p.motion, accent, onMotion)

            Section("Kebenaran")
            RowButton(
                "Lencana notifikasi",
                if (Intents.hasNotificationAccess(context)) "Sudah dibenarkan ✓" else "Benarkan akses notifikasi supaya ikon app tunjuk tanda",
                onNotif,
            )
            RowButton("Lokasi & kalendar", "Untuk cuaca, peta dan acara kalendar", onPerms)
            RowButton("Jadikan Glass launcher utama", "Pilih Glass sebagai app Home", onDefault)

            Section("Susun atur")
            RowButton("Set semula susun atur", "Kembalikan home screen tema ini ke asal", onReset)

            Spacer(Modifier.height(24.dp))
            GText("Glass v1.1 · Liquid Glass Launcher", 11.sp, alpha = 0.45f)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ======================================================= ANIMASI
@Composable
private fun MotionSettings(m: MotionPrefs, accent: Color, onMotion: (MotionPrefs) -> Unit) {
    Section("Animasi")
    ToggleRow("Hidupkan animasi", "Matikan semua untuk jimat bateri", m.enabled, accent) { onMotion(m.copy(enabled = it)) }

    Spacer(Modifier.height(4.dp))
    GText("Kelajuan: " + Motion.speedLabel(m.speed), 14.sp, weight = FontWeight.Medium, alpha = if (m.enabled) 1f else 0.4f)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (v in Motion.SPEEDS) {
            val label = when (v) {
                0.5f -> "0.5x Perlahan"
                1f -> "1x Biasa"
                2f -> "2x Laju"
                else -> Motion.speedLabel(v)
            }
            Chip(label, m.speed == v, accent, enabled = m.enabled) { onMotion(m.copy(speed = v)) }
        }
    }
    Spacer(Modifier.height(12.dp))
    GText("Pratonton (ketik untuk main semula)", 12.sp, alpha = 0.65f)
    Spacer(Modifier.height(6.dp))
    MotionPreview(m, accent)
    Spacer(Modifier.height(12.dp))

    ToggleRow("Kad muncul satu-satu", "Widget naik & pudar masuk bila buka home", m.entrance, accent, m.enabled) { onMotion(m.copy(entrance = it)) }
    ToggleRow("Latar gelombang pastel", "Gelombang lembut bergerak di belakang kaca", m.waves, accent, m.enabled) { onMotion(m.copy(waves = it)) }
    ToggleRow("Skrin \"Mencari…\"", "Animasi gelombang sebelum carian Google dibuka", m.searching, accent, m.enabled) { onMotion(m.copy(searching = it)) }
    ToggleRow("Teks sapaan menaip", "Widget Sapaan ditaip huruf demi huruf", m.typing, accent, m.enabled) { onMotion(m.copy(typing = it)) }
    ToggleRow("Kesan tekan melantun", "Kad mengembang lembut bila dilepaskan", m.press, accent, m.enabled) { onMotion(m.copy(press = it)) }
    GText("Tip: matikan latar gelombang jika bateri cepat habis.", 12.sp, alpha = 0.55f, maxLines = 2)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            GText(title, 15.sp, weight = FontWeight.Medium, alpha = if (enabled) 1f else 0.4f)
            GText(subtitle, 12.sp, alpha = if (enabled) 0.65f else 0.3f, maxLines = 2)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = Color.Black,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.3f),
            ),
        )
    }
    Spacer(Modifier.height(8.dp))
}

// ======================================================= ZON SOLAT
@Composable
fun ZonePickerOverlay(current: String, accent: Color, onPick: (String) -> Unit, onClose: () -> Unit) {
    FullGlass {
        Header("Pilih Zon Waktu Solat", onClose)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(PrayerZones.all, key = { it.first }) { (code, name) ->
                val sel = code == current
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (sel) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                        .clickable { onPick(code) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GText(code, 14.sp, weight = FontWeight.SemiBold, color = if (sel) accent else Color.White, modifier = Modifier.width(64.dp))
                    GText(name, 13.sp, alpha = 0.85f, maxLines = 2, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

