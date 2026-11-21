package com.mohdrozaimi.glass.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import java.io.File
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Wallpaper siap (dilukis dengan kod) + wallpaper pilihan pengguna. */
object Wallpapers {
    val presetNames = listOf("Hutan Kaca", "Ombak Malam", "Senja Cair", "Lautan Dalam", "Aurora")

    private fun file(context: Context) = File(context.filesDir, "wallpaper.jpg")

    fun load(context: Context, spec: String, w: Int, h: Int): Bitmap {
        if (spec == "file") {
            val f = file(context)
            if (f.exists()) decodeSampled(f, w, h)?.let { return it }
        }
        val idx = spec.removePrefix("preset:").toIntOrNull()?.coerceIn(0, presetNames.size - 1) ?: 0
        return renderPreset(idx, w, h)
    }

    /** Salin gambar pilihan ke dalam storan app supaya kekal. */
    fun saveFromUri(context: Context, uri: Uri): Boolean = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file(context).outputStream().use { input.copyTo(it) }
        }
        true
    } catch (e: Exception) {
        false
    }

    private fun decodeSampled(f: File, w: Int, h: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun renderPreset(index: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(max(w, 1), max(h, 1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(index * 31 + 7)
        val W = w.toFloat()
        val H = h.toFloat()

        fun bg(top: Int, bottom: Int) {
            p.shader = LinearGradient(0f, 0f, W * 0.3f, H, top, bottom, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W, H, p)
            p.shader = null
        }

        fun blob(cx: Float, cy: Float, r: Float, color: Int) {
            p.shader = RadialGradient(cx, cy, r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, p)
            p.shader = null
        }

        when (index) {
            0 -> { // Hutan Kaca: daun-daun hijau
                bg(0xFF0B1F14.toInt(), 0xFF14331F.toInt())
                val greens = intArrayOf(0xFF1E5631.toInt(), 0xFF2E7D32.toInt(), 0xFF4C9A2A.toInt(), 0xFF76B947.toInt(), 0xFF0F3D1E.toInt(), 0xFFA4DE02.toInt())
                repeat(220) {
                    val cx = rnd.nextFloat() * W
                    val cy = rnd.nextFloat() * H
                    val lw = W * (0.05f + rnd.nextFloat() * 0.12f)
                    p.color = greens[rnd.nextInt(greens.size)]
                    p.alpha = 150 + rnd.nextInt(105)
                    c.save()
                    c.rotate(rnd.nextFloat() * 360f, cx, cy)
                    c.drawOval(cx - lw, cy - lw * 0.42f, cx + lw, cy + lw * 0.42f, p)
                    c.restore()
                }
                p.alpha = 255
                blob(W * 0.8f, H * 0.15f, W * 0.6f, 0x66FFF59D)
            }
            1 -> { // Ombak Malam: garis bergelombang kelabu
                bg(0xFF0A0A0C.toInt(), 0xFF1A1A1F.toInt())
                p.style = Paint.Style.STROKE
                for (i in 0 until 26) {
                    val path = Path()
                    val base = H * (i / 26f)
                    val amp = H * 0.05f + i * 3f
                    path.moveTo(0f, base)
                    var x = 0f
                    while (x <= W) {
                        path.lineTo(x, base + amp * sin((x / W) * 6.28f + i * 0.35f).toFloat())
                        x += 8f
                    }
                    p.strokeWidth = 3f + (i % 4) * 5f
                    p.color = if (i % 3 == 0) 0x55FFFFFF else 0x22FFFFFF
                    c.drawPath(path, p)
                }
                p.style = Paint.Style.FILL
            }
            2 -> { // Senja Cair
                bg(0xFF2B0F3A.toInt(), 0xFFFF7A59.toInt())
                blob(W * 0.2f, H * 0.25f, W * 0.9f, 0xCCFF4F8B.toInt())
                blob(W * 0.85f, H * 0.55f, W * 0.8f, 0xCCFFB347.toInt())
                blob(W * 0.4f, H * 0.9f, W * 0.7f, 0xAA7B2FF7.toInt())
            }
            3 -> { // Lautan Dalam
                bg(0xFF021526.toInt(), 0xFF03346E.toInt())
                blob(W * 0.75f, H * 0.2f, W * 0.8f, 0xAA00B4D8.toInt())
                blob(W * 0.1f, H * 0.6f, W * 0.9f, 0x9900F5D4.toInt())
                blob(W * 0.6f, H * 0.95f, W * 0.7f, 0x993A0CA3.toInt())
            }
            else -> { // Aurora
                bg(0xFF050816.toInt(), 0xFF0B1026.toInt())
                blob(W * 0.3f, H * 0.3f, W * 0.9f, 0x8800FFA3.toInt())
                blob(W * 0.8f, H * 0.45f, W * 0.8f, 0x88B84DFF.toInt())
                blob(W * 0.5f, H * 0.8f, W * 0.7f, 0x6600C2FF)
                p.color = 0xCCFFFFFF.toInt()
                repeat(120) { c.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H * 0.6f, rnd.nextFloat() * 2.2f, p) }
            }
        }
        return bmp
    }
}
