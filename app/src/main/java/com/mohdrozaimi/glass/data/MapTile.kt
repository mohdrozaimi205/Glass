package com.mohdrozaimi.glass.data

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

/** Kedudukan jubin peta OpenStreetMap (Web Mercator). */
data class TilePos(val zoom: Int, val x: Int, val y: Int, val fx: Double, val fy: Double)

fun tilePos(lat: Double, lon: Double, zoom: Int = 15): TilePos {
    val n = 1 shl zoom
    val xf = (lon + 180.0) / 360.0 * n
    val yf = (1.0 - asinh(tan(lat * PI / 180.0)) / PI) / 2.0 * n
    val x = floor(xf).toInt()
    val y = floor(yf).toInt()
    return TilePos(zoom, x, y, xf - x, yf - y)
}

fun tileUrl(zoom: Int, x: Int, y: Int): String = "https://tile.openstreetmap.org/$zoom/$x/$y.png"

fun tileUrl(lat: Double, lon: Double, zoom: Int = 15): String {
    val t = tilePos(lat, lon, zoom)
    return tileUrl(zoom, t.x, t.y)
}

/** Jubin 2x2 yang mengelilingi lokasi. Pulang (x0, y0) jubin kiri-atas. */
fun tileBlock(t: TilePos): Pair<Int, Int> =
    (if (t.fx < 0.5) t.x - 1 else t.x) to (if (t.fy < 0.5) t.y - 1 else t.y)
