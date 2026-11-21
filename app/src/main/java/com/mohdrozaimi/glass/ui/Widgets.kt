package com.mohdrozaimi.glass.ui

import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mohdrozaimi.glass.data.AppEntry
import com.mohdrozaimi.glass.data.CalEvent
import com.mohdrozaimi.glass.data.HomeItem
import com.mohdrozaimi.glass.data.PRAYER_NAMES
import com.mohdrozaimi.glass.data.PrayerDay
import com.mohdrozaimi.glass.data.Weather
import com.mohdrozaimi.glass.data.dayShortBM
import com.mohdrozaimi.glass.data.formatCountdown
import com.mohdrozaimi.glass.data.formatDateBM
import com.mohdrozaimi.glass.data.nextPrayer
import com.mohdrozaimi.glass.data.prayerToday
import com.mohdrozaimi.glass.data.searchUrl
import com.mohdrozaimi.glass.data.weatherEmoji
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

data class WidgetStyle(val accent: Color, val clockWeight: FontWeight, val name: String)

/** Semua data yang diperlukan widget. */
data class WidgetData(
    val now: LocalDateTime,
    val is24h: Boolean,
    val weather: Weather?,
    val prayer: List<PrayerDay>,
    val zoneName: String,
    val events: List<CalEvent>?,
    val map: MapView?,
    val hasLocation: Boolean,
)

private val TextShadow = Shadow(Color.Black.copy(alpha = 0.40f), Offset(0f, 2f), 10f)

@Composable
fun GText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    alpha: Float = 1f,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.copy(alpha = alpha * color.alpha),
        fontSize = size,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(shadow = TextShadow),
    )
}

fun fmtTime(t: LocalTime, is24h: Boolean): String =
    t.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm"))

// ------------------------------------------------------------------ JAM
@Composable
fun ClockWidget(d: WidgetData, s: WidgetStyle, w: Int, h: Int) {
    val time = fmtTime(d.now.toLocalTime(), d.is24h)
    val small = w == 1 && h == 1
    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val size = when {
            small -> 22.sp
            h >= 2 && w >= 4 -> 72.sp
            h >= 2 -> 44.sp
            else -> 34.sp
        }
        GText(time, size, weight = s.clockWeight)
        if (!small) {
            val wx = d.weather?.let { "  ${weatherEmoji(it.code)} ${it.tempC}°C" } ?: ""
            GText(formatDateBM(d.now.toLocalDate()) + wx, if (w >= 4) 15.sp else 12.sp, alpha = 0.92f)
        }
        if (h >= 2) {
            val ev = d.events?.firstOrNull { !it.start.isBefore(d.now.minusHours(1)) }
            val np = nextPrayer(d.prayer, d.now)
            val line = when {
                ev != null -> {
                    val day = when (ev.start.toLocalDate()) {
                        d.now.toLocalDate() -> if (ev.allDay) "Hari ini" else fmtTime(ev.start.toLocalTime(), d.is24h)
                        d.now.toLocalDate().plusDays(1) -> "Esok"
                        else -> dayShortBM(ev.start.toLocalDate())
                    }
                    "$day · ${ev.title}"
                }
                np != null -> "${np.first} ${fmtTime(np.second.toLocalTime(), d.is24h)}"
                else -> null
            }
            if (line != null) {
                Spacer(Modifier.height(4.dp))
                GText(line, 13.sp, alpha = 0.8f)
            }
        }
    }
}

// ------------------------------------------------------------------ KALENDAR
@Composable
fun CalendarWidget(d: WidgetData, s: WidgetStyle, w: Int, h: Int, onAdd: () -> Unit, onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        val events = d.events
        if (events == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                GText("📅 Kalendar", 14.sp, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Pill("Benarkan akses", s.accent, onGrant)
            }
            return@Box
        }
        if (w == 1 && h == 1) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                GText(dayShortBM(d.now.toLocalDate()), 11.sp, color = s.accent)
                GText("${d.now.dayOfMonth}", 26.sp, weight = FontWeight.SemiBold)
            }
            return@Box
        }
        val maxRows = (h * 2).coerceAtLeast(2)
        val groups = events.groupBy { it.start.toLocalDate() }.entries.take(maxRows)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (groups.isEmpty()) {
                GText("${dayShortBM(d.now.toLocalDate())} ${d.now.dayOfMonth}", 22.sp, weight = FontWeight.SemiBold)
                GText("Tiada acara 7 hari ini", 12.sp, alpha = 0.75f, maxLines = 2)
            }
            var used = 0
            for ((date, list) in groups) {
                if (used >= maxRows) break
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.width(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        GText("${date.dayOfMonth}", 20.sp, weight = FontWeight.SemiBold)
                        GText(dayShortBM(date), 10.sp, alpha = 0.75f)
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (ev in list.take(2)) {
                            used++
                            Row(
                                Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(width = 3.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(s.accent))
                                Spacer(Modifier.width(5.dp))
                                Column {
                                    GText(ev.title, 11.sp, weight = FontWeight.Medium)
                                    GText(if (ev.allDay) "Sepanjang hari" else fmtTime(ev.start.toLocalTime(), d.is24h), 9.sp, alpha = 0.7f)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(s.accent).clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) { Text("+", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

// ------------------------------------------------------------------ CUACA
@Composable
fun WeatherWidget(d: WidgetData, s: WidgetStyle, w: Int, h: Int, onGrant: () -> Unit) {
    val wx = d.weather
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        if (wx == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GText("🌤️ Cuaca", 14.sp, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (!d.hasLocation) Pill("Benarkan lokasi", s.accent, onGrant) else GText("Memuatkan...", 12.sp, alpha = 0.7f)
            }
            return@Box
        }
        if (h == 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GText(weatherEmoji(wx.code), if (w == 1) 18.sp else 24.sp)
                Spacer(Modifier.width(6.dp))
                GText("${wx.tempC}°", if (w == 1) 18.sp else 26.sp, weight = FontWeight.Light)
                if (w >= 2) {
                    Spacer(Modifier.width(8.dp))
                    GText(wx.label, 13.sp, alpha = 0.85f)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GText(weatherEmoji(wx.code), 40.sp)
                GText("${wx.tempC}°", 40.sp, weight = FontWeight.Light)
                GText(wx.label, 13.sp, alpha = 0.85f)
            }
        }
    }
}

// ------------------------------------------------------------------ PETA
@Composable
fun MapWidget(d: WidgetData, s: WidgetStyle, onGrant: () -> Unit) {
    val m = d.map
    if (m == null) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GText("🗺️ Peta", 14.sp, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (!d.hasLocation) Pill("Benarkan lokasi", s.accent, onGrant) else GText("Memuatkan...", 12.sp, alpha = 0.7f)
            }
        }
        return
    }
    Canvas(Modifier.fillMaxSize()) { drawMap(m.image, m.point, s.accent) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMap(img: ImageBitmap, pt: Pair<Float, Float>, accent: Color) {
    val k = max(size.width, size.height) / (img.width / 2f)
    val dw = img.width * k
    val dh = img.height * k
    val left = size.width / 2f - pt.first * dw
    val top = size.height / 2f - pt.second * dh
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
        alpha = 0.92f,
        filterQuality = FilterQuality.Medium,
    )
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(accent.copy(alpha = 0.25f), radius = 22f, center = c)
    drawCircle(Color.White, radius = 11f, center = c)
    drawCircle(accent, radius = 7.5f, center = c)
}

// ------------------------------------------------------------------ CARIAN GOOGLE
@Composable
fun SearchWidget(s: WidgetStyle, w: Int) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    fun go() {
        val url = searchUrl(q) ?: return
        Intents.openUrl(context, url)
        q = ""
        focus.clearFocus()
    }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = "Cari", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp).clickable { go() })
        if (w >= 2) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) GText(if (w >= 4) "Cari di Google..." else "Cari...", 14.sp, alpha = 0.65f)
                BasicTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(s.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { go() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ WAKTU SOLAT
@Composable
fun PrayerWidget(d: WidgetData, s: WidgetStyle, w: Int, h: Int) {
    val np = nextPrayer(d.prayer, d.now)
    Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        if (np == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GText("🕌 Waktu Solat", 14.sp, weight = FontWeight.SemiBold)
                GText("Memuatkan...", 12.sp, alpha = 0.7f)
            }
            return@Box
        }
        val mins = Duration.between(d.now, np.second).toMinutes() + 1
        val time = fmtTime(np.second.toLocalTime(), d.is24h)
        when {
            w == 1 && h == 1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GText(np.first, 11.sp, color = s.accent)
                GText(time, 16.sp, weight = FontWeight.SemiBold)
            }
            h == 1 -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GText("🕌", 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    GText("${np.first}  $time", 15.sp, weight = FontWeight.SemiBold)
                    if (w >= 4) GText(d.zoneName, 10.sp, alpha = 0.65f)
                }
                GText("dalam ${formatCountdown(mins)}", 12.sp, color = s.accent)
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GText("Solat seterusnya", 11.sp, alpha = 0.7f)
                        GText(np.first, if (w >= 4) 30.sp else 22.sp, weight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GText(time, if (w >= 4) 30.sp else 20.sp, weight = FontWeight.Light)
                        GText("dalam ${formatCountdown(mins)}", 12.sp, color = s.accent)
                    }
                }
                if (w >= 4) {
                    val today = prayerToday(d.prayer, np.second.toLocalDate()) ?: prayerToday(d.prayer, LocalDate.now())
                    if (today != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (name in PRAYER_NAMES) {
                                val t = today.times[name] ?: continue
                                val active = name == np.first
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    GText(name, 10.sp, color = if (active) s.accent else Color.White, alpha = if (active) 1f else 0.7f)
                                    GText(fmtTime(t, d.is24h), 13.sp, weight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
                GText(d.zoneName, 10.sp, alpha = 0.6f)
            }
        }
    }
}

// ------------------------------------------------------------------ WIDGET APP LAIN
@Composable
fun SystemWidgetView(item: HomeItem.SystemWidget, widgets: WidgetController) {
    val info = remember(item.appWidgetId) { widgets.info(item.appWidgetId) }
    if (info == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GText("Widget tidak tersedia", 12.sp, alpha = 0.7f) }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        val last = remember { floatArrayOf(0f, 0f) }
        AndroidView(
            factory = { ctx -> widgets.host.createView(ctx, item.appWidgetId, info) },
            update = { v ->
                if (last[0] != wDp || last[1] != hDp) {
                    last[0] = wDp
                    last[1] = hDp
                    try {
                        v.updateAppWidgetSize(Bundle(), listOf(SizeF(wDp, hDp)))
                    } catch (e: Exception) { }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ------------------------------------------------------------------ KECIL-KECIL
@Composable
fun Pill(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(accent).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun AppIconView(app: AppEntry?, badge: Int, iconSize: androidx.compose.ui.unit.Dp, showLabel: Boolean, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            if (app != null) {
                androidx.compose.foundation.Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Box(Modifier.size(iconSize).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    GText("?", 18.sp)
                }
            }
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(0.dp).size(18.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center,
                ) { Text(if (badge > 9) "9+" else "$badge", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            GText(app?.label ?: "Tiada app", 11.sp)
        }
    }
}
