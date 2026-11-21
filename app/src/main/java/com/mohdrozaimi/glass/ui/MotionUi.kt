package com.mohdrozaimi.glass.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdrozaimi.glass.data.Motion
import com.mohdrozaimi.glass.data.MotionPrefs
import com.mohdrozaimi.glass.data.nextPrayer
import com.mohdrozaimi.glass.glass.GlassCard
import com.mohdrozaimi.glass.glass.LocalHomeVisit
import com.mohdrozaimi.glass.glass.LocalMotion
import com.mohdrozaimi.glass.glass.TWO_PI
import com.mohdrozaimi.glass.glass.drawPastelWaves
import kotlinx.coroutines.delay

private val Ink = Color(0xFF23242B)

// ======================================================= SKRIN "MENCARI…"
/**
 * Skrin pastel penuh dengan gelombang bergerak sebelum carian dibuka.
 * Ketik untuk terus; butang Back membatalkan.
 */
@Composable
fun SearchingOverlay(query: String, onGo: () -> Unit) {
    val motion = LocalMotion.current
    val go by rememberUpdatedState(onGo)
    val fade = remember { Animatable(0f) }
    val t = rememberInfiniteTransition(label = "searching")
    val wave by t.animateFloat(0f, TWO_PI, infiniteRepeatable(tween(motion.ms(7000), easing = LinearEasing)), label = "wave")
    val dots by t.animateFloat(0f, 4f, infiniteRepeatable(tween(motion.ms(1400), easing = LinearEasing)), label = "dots")
    val pulse by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(motion.ms(1100)), RepeatMode.Reverse), label = "pulse")

    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(motion.ms(260)))
        delay(motion.ms(1100).toLong())
        go()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F2FB), Color(0xFFE5EAF8), Color(0xFFF3E4F0))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { go() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawPastelWaves(wave, size, Offset.Zero, strength = 2.4f)
        }
        Box(
            Modifier
                .size(210.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent))),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GText("Sebentar ya…", 13.sp, color = Ink, alpha = 0.6f)
            Spacer(Modifier.height(4.dp))
            GText("Mencari" + ".".repeat(dots.toInt().coerceIn(0, 3)), 28.sp, weight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(10.dp))
            GText("“$query”", 15.sp, color = Ink, alpha = 0.7f, maxLines = 2)
        }
        GText(
            "Ketik untuk terus",
            12.sp,
            color = Ink,
            alpha = 0.45f,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
        )
    }
}

// ======================================================= WIDGET SAPAAN
/** Ucapan yang ditaip huruf demi huruf setiap kali home screen dibuka. */
@Composable
fun GreetingWidget(d: WidgetData, s: WidgetStyle, w: Int, h: Int) {
    val motion = LocalMotion.current
    val visit = LocalHomeVisit.current
    val full = Motion.greetingText(d.now.hour)
    var shown by remember { mutableIntStateOf(if (motion.typingOn) 0 else full.length) }
    val extra = remember { Animatable(if (motion.typingOn) 0f else 1f) }

    LaunchedEffect(visit, full, motion.typingOn, motion.speed) {
        if (!motion.typingOn) {
            shown = full.length
            extra.snapTo(1f)
            return@LaunchedEffect
        }
        shown = 0
        extra.snapTo(0f)
        delay(motion.ms(300).toLong())
        while (shown < full.length) {
            delay(motion.ms(38).toLong())
            shown++
        }
        extra.animateTo(1f, tween(motion.ms(400)))
    }

    val typing = shown < full.length
    val np = nextPrayer(d.prayer, d.now)
    val parts = buildList {
        d.events?.firstOrNull { !it.start.isBefore(d.now) || it.allDay }?.let {
            add(if (it.allDay) it.title else "${it.title} · ${fmtTime(it.start.toLocalTime(), d.is24h)}")
        }
        if (np != null) add("${np.first} ${fmtTime(np.second.toLocalTime(), d.is24h)}")
    }
    val big = h >= 2 || w >= 4

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        GText(
            full.take(shown) + if (typing) "▍" else "",
            if (h >= 2) 26.sp else if (big) 18.sp else 15.sp,
            weight = FontWeight.Light,
            maxLines = if (h >= 2) 3 else 2,
        )
        if (parts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            GText(
                parts.joinToString("  ·  "),
                12.sp,
                color = s.accent,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    alpha = extra.value
                    translationY = (1f - extra.value) * 12f
                },
            )
        }
    }
}

// ======================================================= PRATONTON ANIMASI
/** Contoh kecil yang bergerak dalam Tetapan supaya beza kelajuan nampak serta-merta. */
@Composable
fun MotionPreview(m: MotionPrefs, accent: Color) {
    var cycle by remember { mutableIntStateOf(0) }
    LaunchedEffect(m) {
        while (true) {
            delay(m.ms(2800).toLong())
            cycle++
        }
    }
    val text = "Hai! Ini contoh animasi."
    var shown by remember { mutableIntStateOf(text.length) }
    LaunchedEffect(cycle, m) {
        if (!m.typingOn) {
            shown = text.length
            return@LaunchedEffect
        }
        shown = 0
        delay(m.ms(250).toLong())
        while (shown < text.length) {
            delay(m.ms(38).toLong())
            shown++
        }
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        cornerRadius = 22.dp,
        onClick = { cycle++ },
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 3) PreviewPill(i, cycle, m, accent, Modifier.weight(1f))
            }
            GText(text.take(shown) + if (shown < text.length) "▍" else "", 15.sp, weight = FontWeight.Light)
        }
    }
}

@Composable
private fun PreviewPill(i: Int, cycle: Int, m: MotionPrefs, accent: Color, modifier: Modifier) {
    val a = remember { Animatable(1f) }
    LaunchedEffect(cycle, m) {
        if (!m.entranceOn) {
            a.snapTo(1f)
            return@LaunchedEffect
        }
        a.snapTo(0f)
        delay(m.ms(90).toLong() * i)
        a.animateTo(1f, tween(m.ms(560), easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .height(40.dp)
            .graphicsLayer {
                alpha = a.value
                translationY = (1f - a.value) * 28f
                val s = 0.9f + 0.1f * a.value
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (i == 0) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f)),
    )
}
