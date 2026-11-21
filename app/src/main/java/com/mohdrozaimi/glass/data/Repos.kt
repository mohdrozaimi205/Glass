package com.mohdrozaimi.glass.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.coroutines.resume

fun Context.hasPerm(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

object LocationRepo {
    @SuppressLint("MissingPermission")
    suspend fun get(context: Context): Pair<Double, Double>? {
        if (!context.hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val last: Location? = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { pr -> try { lm.getLastKnownLocation(pr) } catch (e: Exception) { null } }
            .maxByOrNull { it.time }
        if (last != null) return last.latitude to last.longitude
        val provider = if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else return null
        val fresh = withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    lm.getCurrentLocation(provider, null, context.mainExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        return fresh?.let { it.latitude to it.longitude }
    }
}

class WeatherRepo(private val settings: Settings) {
    suspend fun get(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&timezone=auto"
            .format(java.util.Locale.US, lat, lon)
        try {
            val json = Net.text(url)
            val w = parseOpenMeteo(json)
            settings.putCache("weather", json)
            w
        } catch (e: Exception) {
            cached()
        }
    }

    suspend fun cached(): Weather? = try {
        settings.cache("weather")?.let { parseOpenMeteo(it) }
    } catch (e: Exception) {
        null
    }
}

class PrayerRepo(private val settings: Settings) {
    /** Waktu solat 7 hari dari hari ini (cache digunakan jika tiada internet). */
    suspend fun get(zone: String): List<PrayerDay> = withContext(Dispatchers.IO) {
        val key = "prayer_$zone"
        val cached = try { settings.cache(key)?.let { parseJakim(it) } } catch (e: Exception) { null } ?: emptyList()
        val tomorrow = LocalDate.now().plusDays(1)
        if (cached.any { it.date == tomorrow }) return@withContext cached
        try {
            val json = Net.text("https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=$zone")
            val days = parseJakim(json)
            if (days.isNotEmpty()) settings.putCache(key, json)
            days.ifEmpty { cached }
        } catch (e: Exception) {
            cached
        }
    }
}

data class CalEvent(val title: String, val start: LocalDateTime, val allDay: Boolean)

object CalendarRepo {
    /** null = tiada kebenaran. */
    fun upcoming(context: Context, days: Int = 7): List<CalEvent>? {
        if (!context.hasPerm(Manifest.permission.READ_CALENDAR)) return null
        val now = System.currentTimeMillis()
        val end = now + days * 24L * 3600_000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString()).appendPath(end.toString()).build()
        val proj = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY)
        val out = ArrayList<CalEvent>()
        try {
            context.contentResolver.query(uri, proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext() && out.size < 20) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(Tiada tajuk)"
                    val begin = c.getLong(1)
                    val allDay = c.getInt(2) == 1
                    val zone = if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
                    out += CalEvent(title, LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), zone), allDay)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out
    }
}

object MapRepo {
    /** Peta 2x2 jubin di sekitar lokasi. Pulang (gambar, kedudukan titik dalam gambar 0..1). */
    suspend fun get(context: Context, lat: Double, lon: Double): Pair<Bitmap, Pair<Float, Float>>? = withContext(Dispatchers.IO) {
        val t = tilePos(lat, lon, 15)
        val (x0, y0) = tileBlock(t)
        val px = ((t.x - x0 + t.fx) / 2.0).toFloat()
        val py = ((t.y - y0 + t.fy) / 2.0).toFloat()
        val f = File(context.filesDir, "map_${t.zoom}_${x0}_$y0.png")
        val cached = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
        if (cached != null) return@withContext cached to (px to py)
        try {
            val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            for (dy in 0..1) for (dx in 0..1) {
                val b = Net.bytes(tileUrl(t.zoom, x0 + dx, y0 + dy))
                val tile = BitmapFactory.decodeByteArray(b, 0, b.size) ?: continue
                c.drawBitmap(tile, dx * 256f, dy * 256f, null)
            }
            context.filesDir.listFiles { file -> file.name.startsWith("map_") }?.forEach { it.delete() }
            f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            out to (px to py)
        } catch (e: Exception) {
            null
        }
    }
}
