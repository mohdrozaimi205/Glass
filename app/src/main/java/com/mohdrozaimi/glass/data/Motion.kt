package com.mohdrozaimi.glass.data

import kotlin.math.roundToInt

/** Tetapan animasi yang dipilih pengguna dalam Tetapan → Animasi. */
data class MotionPrefs(
    val speed: Float = 1f,
    val enabled: Boolean = true,
    val entrance: Boolean = true,
    val waves: Boolean = true,
    val searching: Boolean = true,
    val typing: Boolean = true,
    val press: Boolean = true,
) {
    val entranceOn: Boolean get() = enabled && entrance
    val wavesOn: Boolean get() = enabled && waves
    val searchingOn: Boolean get() = enabled && searching
    val typingOn: Boolean get() = enabled && typing
    val pressOn: Boolean get() = enabled && press

    /** Tempoh animasi (ms) selepas ikut kelajuan pilihan. */
    fun ms(baseMs: Int): Int = Motion.scale(baseMs, speed)
}

/** Saiz ikon app & widget 1x1: "S" Kecil, "M" Sederhana, "L" Besar. */
object IconSize {
    val KEYS = listOf("S", "M", "L")
    fun dp(key: String): Int = when (key) {
        "S" -> 46
        "L" -> 62
        else -> 54
    }
    fun label(key: String): String = when (key) {
        "S" -> "Kecil"
        "L" -> "Besar"
        else -> "Sederhana"
    }
}

object Motion {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2f
    val SPEEDS: List<Float> = listOf(0.5f, 0.75f, 1f, 1.5f, 2f)

    fun clampSpeed(s: Float): Float = if (s.isNaN()) 1f else s.coerceIn(MIN_SPEED, MAX_SPEED)

    /** Kelajuan 2x = separuh masa; 0.5x = dua kali ganda masa. */
    fun scale(baseMs: Int, speed: Float): Int = (baseMs / clampSpeed(speed)).roundToInt().coerceAtLeast(1)

    fun speedLabel(s: Float): String {
        val r = (s * 100).roundToInt()
        val num = if (r % 100 == 0) "${r / 100}" else if (r % 10 == 0) "${r / 100}.${(r % 100) / 10}" else "${r / 100}.${r % 100}"
        return "${num}x"
    }

    /** Ucapan ikut waktu: pagi 5–11, tengah hari 12–13, petang 14–18, malam selainnya. */
    fun salam(hour: Int): String = when (hour) {
        in 5..11 -> "Selamat pagi"
        in 12..13 -> "Selamat tengah hari"
        in 14..18 -> "Selamat petang"
        else -> "Selamat malam"
    }

    fun greetingText(hour: Int): String = "${salam(hour)}. Ini agenda anda hari ini."
}
