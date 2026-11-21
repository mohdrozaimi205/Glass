package com.mohdrozaimi.glass.data

import java.time.LocalDate

private val DAYS = listOf("Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu", "Ahad")
private val DAYS_SHORT = listOf("Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd")
private val MONTHS = listOf(
    "Januari", "Februari", "Mac", "April", "Mei", "Jun",
    "Julai", "Ogos", "September", "Oktober", "November", "Disember",
)

fun dayNameBM(d: LocalDate): String = DAYS[d.dayOfWeek.value - 1]
fun dayShortBM(d: LocalDate): String = DAYS_SHORT[d.dayOfWeek.value - 1]
fun monthNameBM(d: LocalDate): String = MONTHS[d.monthValue - 1]

/** "Sabtu, 21 November" */
fun formatDateBM(d: LocalDate): String = "${dayNameBM(d)}, ${d.dayOfMonth} ${monthNameBM(d)}"
