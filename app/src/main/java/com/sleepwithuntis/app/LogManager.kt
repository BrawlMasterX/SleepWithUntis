package com.sleepwithuntis.app

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private const val LOG_FILE_NAME = "app_logs.txt"

    fun log(context: Context, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $message\n"
        
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            FileOutputStream(file, true).use { fos ->
                fos.write(logLine.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists()) {
            file.readText()
        } else {
            "Noch keine Logs vorhanden."
        }
    }

    fun clearLogs(context: Context) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}
