package com.mohdrozaimi.glass.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Muat turun APK terbaru dengan DownloadManager dan buka skrin pemasangan Android. */
class Updater(private val context: Context) {
    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun target(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Glass-update.apk")

    fun currentVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    } catch (e: Exception) {
        "0"
    }

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) { Update.parse(Net.text(Update.LATEST_URL)) }

    /**
     * Muat turun dan laporkan kemajuan 0..1. Pulangkan fail APK jika berjaya.
     * Melontar ralat jika muat turun gagal.
     */
    suspend fun download(r: ReleaseInfo, onProgress: (Float) -> Unit): File {
        val file = target()
        withContext(Dispatchers.IO) { if (file.exists()) file.delete() }
        val req = DownloadManager.Request(Uri.parse(r.apkUrl))
            .setTitle("Glass v${r.version}")
            .setDescription("Memuat turun kemas kini Glass")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
        val id = dm.enqueue(req)
        while (true) {
            delay(300)
            val st = withContext(Dispatchers.IO) {
                dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                    if (!c.moveToFirst()) {
                        null
                    } else {
                        Triple(
                            c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                        )
                    }
                }
            } ?: throw IllegalStateException("Muat turun dibatalkan")
            val (status, done, total) = st
            val size = if (total > 0) total else r.sizeBytes
            if (size > 0) onProgress((done.toFloat() / size).coerceIn(0f, 1f))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    onProgress(1f)
                    return file
                }
                DownloadManager.STATUS_FAILED -> throw IllegalStateException("Muat turun gagal")
            }
        }
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Buka tetapan "Pasang app tidak diketahui" untuk Glass. */
    fun openInstallPermission() {
        val i = Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
