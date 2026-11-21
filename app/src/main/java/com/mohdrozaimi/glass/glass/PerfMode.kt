package com.mohdrozaimi.glass.glass

/** Menentukan sama ada Mod Jimat (kesan kaca ringan) patut dihidupkan. */
object PerfMode {
    const val GB: Long = 1024L * 1024L * 1024L
    private const val THRESHOLD: Long = 7L * GB

    fun shouldLite(totalMemBytes: Long, isLowRam: Boolean): Boolean =
        isLowRam || totalMemBytes < THRESHOLD

    /** setting: "auto", "on" atau "off". */
    fun resolve(setting: String, auto: Boolean): Boolean = when (setting) {
        "on" -> true
        "off" -> false
        else -> auto
    }
}
