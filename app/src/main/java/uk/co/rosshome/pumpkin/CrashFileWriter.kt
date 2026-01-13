package uk.co.rosshome.pumpkin

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

object CrashFileWriter {
    fun write(context: Context, payload: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeToDownloadsMediaStore(context, payload)
            } else {
                writeToAppDownloads(context, payload)
            }
        }.onFailure {
            runCatching { writeToAppDownloads(context, payload) }
        }
    }

    private fun writeToDownloadsMediaStore(context: Context, payload: String) {
        val filename = "pumpkin-crash-${Instant.now().toEpochMilli()}.log"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Pumpkin")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(payload.toByteArray())
        }
    }

    private fun writeToAppDownloads(context: Context, payload: String) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "pumpkin-crash-last.log")
        FileOutputStream(file, false).use { stream ->
            stream.write(payload.toByteArray())
        }
    }
}
