package com.mohdrozaimi.glass.notif

/** Kira notifikasi ikut pakej. Pasangan = (pakej, ongoing). Notifikasi ongoing diabaikan. */
fun countBadges(items: List<Pair<String, Boolean>>): Map<String, Int> =
    items.filter { !it.second }.groupingBy { it.first }.eachCount()
