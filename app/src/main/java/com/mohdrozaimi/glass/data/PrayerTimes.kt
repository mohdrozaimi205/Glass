package com.mohdrozaimi.glass.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

val PRAYER_NAMES = listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")

data class PrayerDay(val date: LocalDate, val times: Map<String, LocalTime>)

/** Solat seterusnya selepas [now], atau null jika data tidak cukup. */
fun nextPrayer(days: List<PrayerDay>, now: LocalDateTime): Pair<String, LocalDateTime>? {
    for (day in days.sortedBy { it.date }) {
        for (name in PRAYER_NAMES) {
            val t = day.times[name] ?: continue
            val dt = LocalDateTime.of(day.date, t)
            if (dt.isAfter(now)) return name to dt
        }
    }
    return null
}

fun prayerToday(days: List<PrayerDay>, today: LocalDate): PrayerDay? = days.firstOrNull { it.date == today }

/** "1j 20m" atau "45m". */
fun formatCountdown(minutes: Long): String {
    val m = if (minutes < 0) 0 else minutes
    val h = m / 60
    val r = m % 60
    return if (h > 0) "${h}j ${r}m" else "${r}m"
}
