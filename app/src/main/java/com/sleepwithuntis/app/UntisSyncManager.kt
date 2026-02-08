package com.sleepwithuntis.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import webuntisjava.UntisClient

class UntisSyncManager(private val context: Context) {

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val server = pref.getString("server", "") ?: ""
            val school = pref.getString("school", "") ?: ""
            val user = pref.getString("username", "") ?: ""
            val pass = pref.getString("password", "") ?: ""

            if (server.isEmpty() || user.isEmpty()) return@withContext false

            val client = UntisClient(user, pass, school, server)
            if (!client.login()) return@withContext false

            // Daten abrufen
            val times = client.getWeekTimes()
            val lessons = client.getWeekLessons()

            client.logout()

            // Daten speichern
            saveSettingsTime(times)
            saveSettingsLessons(lessons)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveSettingsTime(map: Map<String, String>) {
        val pref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        pref.edit().apply {
            putString("wake_up_time_montag", map["montag"] ?: "08:00")
            putString("wake_up_time_dienstag", map["dienstag"] ?: "08:00")
            putString("wake_up_time_mittwoch", map["mittwoch"] ?: "08:00")
            putString("wake_up_time_donnerstag", map["donnerstag"] ?: "08:00")
            putString("wake_up_time_freitag", map["freitag"] ?: "08:00")
            apply()
        }
    }

    private fun saveSettingsLessons(map: Map<String, Int>) {
        val pref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        with(pref.edit()) {
            // Wir wandeln den Wert sicher in einen Int um (Standardwert 1)
            putInt("wake_up_lesson_montag", (map["montag"] as? Number)?.toInt() ?: -1)
            putInt("wake_up_lesson_dienstag", (map["dienstag"] as? Number)?.toInt() ?: -1)
            putInt("wake_up_lesson_mittwoch", (map["mittwoch"] as? Number)?.toInt() ?: -1)
            putInt("wake_up_lesson_donnerstag", (map["donnerstag"] as? Number)?.toInt() ?: -1)
            putInt("wake_up_lesson_freitag", (map["freitag"] as? Number)?.toInt() ?: -1)
            apply()
        }
    }
}