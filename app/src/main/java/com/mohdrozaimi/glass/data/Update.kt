package com.mohdrozaimi.glass.data

import org.json.JSONObject

/** Maklumat versi terbaru di GitHub Releases. */
data class ReleaseInfo(
    val version: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val notes: String,
)

object Update {
    const val LATEST_URL = "https://api.github.com/repos/mohdrozaimi205/Glass/releases/latest"

    /** Baca jawapan API GitHub; null jika tiada fail APK. */
    fun parse(json: String): ReleaseInfo? {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").trim()
        if (tag.isEmpty()) return null
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return ReleaseInfo(
                    version = tag.removePrefix("v").removePrefix("V"),
                    apkUrl = a.optString("browser_download_url"),
                    sizeBytes = a.optLong("size", 0L),
                    notes = o.optString("body").trim(),
                )
            }
        }
        return null
    }

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").split('.', '-', ' ').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** true jika [remote] lebih baru daripada [current] (cth "1.1.7" > "1.1.4", "1.2.0" > "1.1.9"). */
    fun isNewer(remote: String, current: String): Boolean {
        val a = parts(remote)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun sizeLabel(bytes: Long): String =
        if (bytes <= 0) "" else String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
}
