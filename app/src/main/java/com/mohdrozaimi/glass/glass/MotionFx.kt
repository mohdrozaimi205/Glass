package com.mohdrozaimi.glass.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mohdrozaimi.glass.data.MotionPrefs
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Tetapan animasi semasa untuk seluruh launcher. */
val LocalMotion = staticCompositionLocalOf { MotionPrefs() }

/** Masa gelombang (0..2π, berulang). Null bila latar gelombang dimatikan. */
val LocalWaveTime = staticCompositionLocalOf<State<Float>?> { null }

/** Bertambah setiap kali home screen dibuka semula; mencetuskan animasi masuk. */
val LocalHomeVisit = compositionLocalOf { 0 }

const val TWO_PI = (2 * PI).toFloat()

private val WaveColors = listOf(
    Color(0xFFA9C7FF), // biru lembut
    Color(0xFFCBB8FF), // ungu lembut
    Color(0xFFFFC3DD), // merah jambu lembut
)

/**
 * Lukis gelombang pastel yang bergerak perlahan, selari dengan kedudukan [origin] di skrin
 * supaya gelombang di dalam kad kaca bersambung dengan gelombang di latar.
 */
fun DrawScope.drawPastelWaves(t: Float, screen: Size, origin: Offset, strength: Float = 1f) {
    if (screen.width <= 0f || screen.height <= 0f) return
    val w = screen.width
    val h = screen.height

    // Cahaya lembut yang beralih perlahan
    val cx = w * (0.5f + 0.32f * cos(t)) - origin.x
    val cy = h * (0.30f + 0.10f * sin(t)) - origin.y
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFD6EC).copy(alpha = 0.22f * strength), Color.Transparent),
            center = Offset(cx, cy),
            radius = w * 0.75f,
        ),
        radius = w * 0.75f,
        center = Offset(cx, cy),
    )

    val steps = 28
    for (i in WaveColors.indices) {
        val base = h * (0.42f + 0.17f * i)
        val amp = h * (0.045f + 0.012f * i)
        val k = TWO_PI / (w * (1.25f + 0.35f * i))
        // Gandaan integer pada t supaya gelung bersambung lancar
        val phase = t * (if (i == 1) -1f else (i + 1).toFloat()) + i * 1.3f
        val path = Path()
        for (s in 0..steps) {
            val x = w * s / steps
            val y = base + amp * sin(x * k + phase) + amp * 0.35f * sin(x * k * 2.3f - phase)
            if (s == 0) path.moveTo(x - origin.x, y - origin.y) else path.lineTo(x - origin.x, y - origin.y)
        }
        path.lineTo(w - origin.x, h - origin.y)
        path.lineTo(-origin.x, h - origin.y)
        path.close()
        val c = WaveColors[i]
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(c.copy(alpha = 0.26f * strength), c.copy(alpha = 0.04f * strength)),
                startY = base - amp - origin.y,
                endY = h - origin.y,
            ),
        )
    }
}

/**
 * Animasi masuk: item pudar masuk sambil naik perlahan, satu demi satu ikut [order].
 * Dimainkan semula setiap kali home screen dibuka.
 */
@Composable
fun Modifier.entrance(order: Int): Modifier {
    val motion = LocalMotion.current
    val visit = LocalHomeVisit.current
    val appear = remember { Animatable(if (motion.entranceOn) 0f else 1f) }
    LaunchedEffect(visit, motion.entranceOn) {
        if (!motion.entranceOn) {
            appear.snapTo(1f)
            return@LaunchedEffect
        }
        appear.snapTo(0f)
        delay(motion.ms(70).toLong() * order.coerceAtMost(12))
        appear.animateTo(1f, tween(motion.ms(560), easing = FastOutSlowInEasing))
    }
    val lift = with(LocalDensity.current) { 36.dp.toPx() }
    return this.graphicsLayer {
        val a = appear.value
        alpha = a
        translationY = (1f - a) * lift
        val s = 0.94f + 0.06f * a
        scaleX = s
        scaleY = s
    }
}
