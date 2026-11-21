package com.mohdrozaimi.glass.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdrozaimi.glass.data.WidgetKind
import com.mohdrozaimi.glass.data.nextPrayer
import com.mohdrozaimi.glass.data.weatherEmoji
import com.mohdrozaimi.glass.glass.GlassCard
import java.time.format.DateTimeFormatter

/** Widget saiz 1x1: jubin kaca sebesar ikon app, dengan label di bawah (sama macam ikon app). */
@Composable
fun MiniWidgetIcon(
    kind: WidgetKind,
    d: WidgetData,
    s: WidgetStyle,
    iconSize: Dp,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
) {
    val scale = iconSize.value / 54f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(
            modifier = Modifier.size(iconSize),
            cornerRadius = iconSize * 0.3f,
            blur = 14.dp,
            onClick = onClick,
            onLongClick = onLongClick,
        ) {
            Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                when (kind) {
                    WidgetKind.CLOCK -> GText(
                        d.now.format(DateTimeFormatter.ofPattern(if (d.is24h) "HH:mm" else "h:mm")),
                        (15 * scale).sp, weight = FontWeight.SemiBold, color = s.accent,
                    )
                    WidgetKind.CALENDAR -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GText(d.now.format(DateTimeFormatter.ofPattern("EEE", java.util.Locale("ms"))), (9 * scale).sp, color = s.accent)
                        GText("${d.now.dayOfMonth}", (20 * scale).sp, weight = FontWeight.SemiBold)
                    }
                    WidgetKind.WEATHER -> {
                        val w = d.weather
                        if (w == null) GText("⛅", (22 * scale).sp)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GText(weatherEmoji(w.code), (16 * scale).sp)
                            GText("${w.tempC}°", (13 * scale).sp, weight = FontWeight.SemiBold)
                        }
                    }
                    WidgetKind.PRAYER -> {
                        val np = nextPrayer(d.prayer, d.now)
                        if (np == null) GText("🕌", (22 * scale).sp)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GText(np.first, (9 * scale).sp, color = s.accent)
                            GText(np.second.format(DateTimeFormatter.ofPattern(if (d.is24h) "HH:mm" else "h:mm")), (13 * scale).sp, weight = FontWeight.SemiBold)
                        }
                    }
                    else -> GText(kindEmoji(kind), (22 * scale).sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        GText(kind.label, 11.sp)
    }
}
