package com.mohdrozaimi.glass.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Net {
    fun bytes(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", "Glass-Launcher/1.0 (Android; personal use)")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    fun text(url: String): String = bytes(url).toString(Charsets.UTF_8)
}
