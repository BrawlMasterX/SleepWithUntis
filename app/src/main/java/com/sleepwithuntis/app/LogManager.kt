package com.sleepwithuntis.app

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private const val PREFIX = "log_"
    private const val SUFFIX = ".txt"
    private const val OLD_LOG_FILE = "app_logs.txt" // Für Migration

    fun log(context: Context, message: String) {
        val now = Date()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        val fileName = "$PREFIX${dayFormat.format(now)}$SUFFIX"
        val logLine = "[${timeFormat.format(now)}] $message\n"
        
        try {
            val file = File(context.filesDir, fileName)
            file.appendText(logLine)
            
            // Einmal täglich bzw. sporadisch aufräumen (ca. bei jedem 10. Log-Aufruf)
            if (Random().nextInt(10) == 0) {
                cleanup(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        val files = context.filesDir.listFiles { _, name -> name.startsWith(PREFIX) && name.endsWith(SUFFIX) }
            ?.sortedBy { it.name }
        
        val sb = StringBuilder()
        
        // Falls noch alte Logs existieren, diese zuerst anzeigen
        val oldFile = File(context.filesDir, OLD_LOG_FILE)
        if (oldFile.exists()) {
            sb.append("--- ALTE LOGS ---\n")
            sb.append(oldFile.readText())
            sb.append("\n\n")
        }

        if (files.isNullOrEmpty() && !oldFile.exists()) {
            return "Noch keine Logs vorhanden."
        }
        
        files?.forEach { file ->
            val dateStr = file.name.removePrefix(PREFIX).removeSuffix(SUFFIX)
            sb.append("--- TAG: $dateStr ---\n")
            sb.append(file.readText())
            sb.append("\n")
        }
        return sb.toString()
    }

    fun cleanup(context: Context) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Neue Tages-Logs prüfen
        context.filesDir.listFiles { _, name -> name.startsWith(PREFIX) && name.endsWith(SUFFIX) }?.forEach { file ->
            try {
                val dateStr = file.name.removePrefix(PREFIX).removeSuffix(SUFFIX)
                val fileDate = dateFormat.parse(dateStr)
                if (fileDate != null && fileDate.before(sevenDaysAgo)) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Fehler beim Parsen ignorieren
            }
        }
        
        // Die ganz alte app_logs.txt löschen, wenn sie älter als 7 Tage ist
        val oldFile = File(context.filesDir, OLD_LOG_FILE)
        if (oldFile.exists()) {
            val lastModified = Date(oldFile.lastModified())
            if (lastModified.before(sevenDaysAgo)) {
                oldFile.delete()
            }
        }
    }

    fun clearLogs(context: Context) {
        context.filesDir.listFiles { _, name -> name.startsWith(PREFIX) && name.endsWith(SUFFIX) }?.forEach { it.delete() }
        val oldFile = File(context.filesDir, OLD_LOG_FILE)
        if (oldFile.exists()) oldFile.delete()
    }
}
