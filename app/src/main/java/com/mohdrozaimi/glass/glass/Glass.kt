package com.mohdrozaimi.glass.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/** Wallpaper yang dilukis oleh launcher sendiri; kad kaca "melihat" imej ini di belakangnya. */
data class Backdrop(val image: ImageBitmap, val screen: Size, val lite: Boolean, val dim: Float = 0.12f)

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Lukis wallpaper (gaya crop penuh skrin), dianjak supaya selari dengan kedudukan [origin] di skrin. */
fun DrawScope.drawBackdrop(bd: Backdrop, origin: Offset) {
    val img = bd.image
    if (img.width == 0 || img.height == 0 || bd.screen.width <= 0f) return
    val scale = max(bd.screen.width / img.width, bd.screen.height / img.height)
    val w = img.width * scale
    val h = img.height * scale
    val left = (bd.screen.width - w) / 2f - origin.x
    val top = (bd.screen.height - h) / 2f - origin.y
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
        filterQuality = FilterQuality.Low,
    )
    if (bd.dim > 0f) drawRect(Color.Black.copy(alpha = bd.dim))
}

/**
 * Kad liquid glass: wallpaper di belakang dikaburkan, dibiaskan di tepi dan diberi kilauan.
 * Dalam Mod Jimat hanya blur digunakan.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    blur: Dp = 22.dp,
    tint: Color = Color.White.copy(alpha = 0.10f),
    refraction: Float = 0.35f,
    pressEffect: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val bd = LocalBackdrop.current
    val density = LocalDensity.current
    val glassShape = RoundedCornerShape(cornerRadius)
    var pos by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val press = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val shader = remember { RuntimeShader(GlassShaders.REFRACTION) }

    val motion = LocalMotion.current
    LaunchedEffect(interaction, pressEffect, motion) {
        if (!pressEffect) return@LaunchedEffect
        interaction.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press ->
                    if (motion.enabled) press.animateTo(1f, tween(motion.ms(120))) else press.snapTo(1f)
                is PressInteraction.Release, is PressInteraction.Cancel -> when {
                    // Lantunan: kad mengembang sedikit melepasi saiz asal, kemudian kembali
                    motion.pressOn -> press.animateTo(
                        0f,
                        spring(dampingRatio = 0.32f, stiffness = 420f * motion.speed * motion.speed),
                    )
                    motion.enabled -> press.animateTo(0f, tween(motion.ms(260)))
                    else -> press.snapTo(0f)
                }
            }
        }
    }

    val clickMod = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onLongClick = onLongClick,
            onClick = { onClick?.invoke() },
        )
    } else Modifier

    Box(
        modifier
            .graphicsLayer {
                val s = 1f - (if (motion.pressOn) 0.045f else 0.03f) * press.value
                scaleX = s
                scaleY = s
            }
            .onGloballyPositioned {
                pos = it.positionInRoot()
                cardSize = it.size
            }
            .then(clickMod),
    ) {
        // Lapisan kaca: wallpaper di belakang + kesan
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = glassShape
                    val blurPx = with(density) { blur.toPx() }
                    val lite = bd?.lite ?: true
                    val blurFx = RenderEffect.createBlurEffect(
                        if (lite) blurPx * 0.8f else blurPx,
                        if (lite) blurPx * 0.8f else blurPx,
                        Shader.TileMode.CLAMP,
                    )
                    renderEffect = if (lite || cardSize.width == 0 || cardSize.height == 0) {
                        blurFx.asComposeRenderEffect()
                    } else {
                        shader.setFloatUniform("size", cardSize.width.toFloat(), cardSize.height.toFloat())
                        shader.setFloatUniform("radius", with(density) { cornerRadius.toPx() })
                        shader.setFloatUniform("strength", refraction + 0.2f * press.value.coerceAtLeast(0f))
                        RenderEffect.createChainEffect(
                            RenderEffect.createRuntimeShaderEffect(shader, "content"),
                            blurFx,
                        ).asComposeRenderEffect()
                    }
                }
                .drawBehind { if (bd != null) drawBackdrop(bd, pos) },
        )
        // Warna kaca + bingkai bercahaya
        Box(
            Modifier
                .matchParentSize()
                .clip(glassShape)
                .background(tint)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.22f)),
                    ),
                    glassShape,
                ),
        )
        Box(Modifier.matchParentSize().clip(glassShape), content = content)
    }
}
