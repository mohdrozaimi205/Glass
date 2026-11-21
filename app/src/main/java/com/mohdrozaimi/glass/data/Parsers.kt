package com.mohdrozaimi.glass.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class Weather(val tempC: Int, val code: Int, val label: String)

fun weatherLabel(code: Int): String = when (code) {
    0 -> "Cerah"
    1, 2 -> "Sebahagian berawan"
    3 -> "Berawan"
    45, 48 -> "Berkabus"
    in 51..57 -> "Gerimis"
    in 61..67, in 80..82 -> "Hujan"
    in 71..77, 85, 86 -> "Salji"
    in 95..99 -> "Ribut petir"
    else -> "-"
}

fun weatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67, in 80..82 -> "🌧️"
    in 71..77, 85, 86 -> "❄️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

/** Baca jawapan Open-Meteo (`current=temperature_2m,weather_code`). */
fun parseOpenMeteo(json: String): Weather {
    val cur = JSONObject(json).getJSONObject("current")
    val temp = cur.getDouble("temperature_2m").roundToInt()
    val code = cur.getInt("weather_code")
    return Weather(temp, code, weatherLabel(code))
}

private val JAKIM_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

/** Baca jawapan JAKIM e-Solat (esolatApi/TakwimSolat). */
fun parseJakim(json: String): List<PrayerDay> {
    val arr = JSONObject(json).getJSONArray("prayerTime")
    val out = ArrayList<PrayerDay>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val date = try {
            LocalDate.parse(o.getString("date"), JAKIM_DATE)
        } catch (e: Exception) {
            continue
        }
        fun t(key: String): LocalTime? = try {
            LocalTime.parse(o.getString(key))
        } catch (e: Exception) {
            null
        }
        val times = linkedMapOf<String, LocalTime>()
        t("fajr")?.let { times["Subuh"] = it }
        t("dhuhr")?.let { times["Zohor"] = it }
        t("asr")?.let { times["Asar"] = it }
        t("maghrib")?.let { times["Maghrib"] = it }
        t("isha")?.let { times["Isyak"] = it }
        out += PrayerDay(date, times)
    }
    return out
}
