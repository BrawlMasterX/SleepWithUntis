package com.sleepwithuntis.app

import android.content.Context
import webuntisjava.UntisClient
import java.time.LocalDate
import java.time.LocalTime

class AlarmCalculator(private val context: Context) {

    /**
     * Berechnet die Weckzeit.
     * @param targetDate Das Datum, für das berechnet wird.
     * @param minTime Wenn gesetzt, werden nur Stunden berücksichtigt, deren Weckzeit NACH dieser Zeit liegt.
     */
    fun getAlarmTime(targetDate: LocalDate, minTime: LocalTime? = null): IntArray? {
        val alarmPrefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val useAbsoluteTime = alarmPrefs.getBoolean("use_absolute_time", false)
        LogManager.log(context, "AlarmCalculator: Berechne Zeit für $targetDate (Modus: ${if (useAbsoluteTime) "Absolut" else "Früher"})")

        if (useAbsoluteTime) {
            try {
                val client = UntisClient(
                    userPrefs.getString("username", "") ?: "",
                    userPrefs.getString("password", "") ?: "",
                    userPrefs.getString("school", "") ?: "",
                    userPrefs.getString("server", "") ?: ""
                )
                if (client.login()) {
                    // Wir holen uns die erste Stunde. 
                    // Hinweis: Absolute Zeit ignoriert minTime hier noch, 
                    // da die Logik meist auf festen Stundenplänen basiert.
                    var firstLesson = client.getFirstLessonForDate(targetDate, true)
                    client.logout()
                    
                    if (firstLesson != -1) {
                        if (firstLesson > 8) { firstLesson = 8 }
                        val firstHour = alarmPrefs.getString("time_stunde_$firstLesson", "08:00")
                        
                        val parts = firstHour?.split(":")
                        val h = parts?.get(0)?.toIntOrNull()
                        val m = parts?.get(1)?.toIntOrNull()
                        
                        if (h != null && m != null) {
                            val resultTime = LocalTime.of(h, m)
                            if (minTime != null && resultTime.isBefore(minTime)) {
                                LogManager.log(context, "AlarmCalculator: Zeit $resultTime liegt vor minTime $minTime. Ignoriere.")
                                return intArrayOf(-1, -1)
                            }
                            return intArrayOf(h, m)
                        }
                    }
                    return intArrayOf(-1, -1)
                }
            } catch (e: Exception) {
                LogManager.log(context, "AlarmCalculator: Fehler bei Online-Abfrage (Absolut): ${e.message}")
                return useOfflineFallbackAbsolute(targetDate)
            }
        } else {
            val earlyMinutes = alarmPrefs.getInt("early_minutes", 100).toLong()
            try {
                val client = UntisClient(
                    userPrefs.getString("username", "") ?: "",
                    userPrefs.getString("password", "") ?: "",
                    userPrefs.getString("school", "") ?: "",
                    userPrefs.getString("server", "") ?: ""
                )

                if (client.login()) {
                    val firstHourList = client.getFirstHourForDate(targetDate, true)
                    client.logout()
                    
                    if (firstHourList[0] != -1) {
                        val firstHourTime = LocalTime.of(firstHourList[0], firstHourList[1])
                        val alarmTime = firstHourTime.minusMinutes(earlyMinutes)
                        
                        if (minTime != null && alarmTime.isBefore(minTime)) {
                            LogManager.log(context, "AlarmCalculator: Berechnete Zeit $alarmTime liegt vor minTime $minTime. Suche evtl. nächste Stunde...")
                            // Da UntisClient nur die ERSTE Stunde liefert, brechen wir hier ab.
                            // Das verhindert das "Nach-Wecken" am Mittag.
                            return intArrayOf(-1, -1)
                        }
                        
                        LogManager.log(context, "AlarmCalculator: Unterrichtsbeginn $firstHourTime. Wecken um $alarmTime")
                        return intArrayOf(alarmTime.hour, alarmTime.minute)
                    }
                    return intArrayOf(-1, -1)
                }
            } catch (e: Exception) {
                LogManager.log(context, "AlarmCalculator: Fehler bei Online-Abfrage (Früher): ${e.message}")
                return useOfflineFallbackEarly(earlyMinutes, targetDate)
            }
        }
        return intArrayOf(-1, -1)
    }

    private fun useOfflineFallbackEarly(early: Long, targetDate: LocalDate): IntArray {
        val alarmPref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val dayString = getDayString(targetDate) ?: return intArrayOf(-1, -1)
        
        val offlineTimeStr = alarmPref.getString("wake_up_time_$dayString", "08:05") ?: "08:05"
        val parts = offlineTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 5
        
        if (h != -1) {
            val firstHourTime = LocalTime.of(h, m)
            val alarmTime = firstHourTime.minusMinutes(early)
            return intArrayOf(alarmTime.hour, alarmTime.minute)
        }
        return intArrayOf(-1, -1)
    }

    private fun useOfflineFallbackAbsolute(targetDate: LocalDate): IntArray {
        val alarmPref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val dayString = getDayString(targetDate) ?: return intArrayOf(-1, -1)

        val savedLesson = alarmPref.getInt("wake_up_lesson_$dayString", 1)
        val lessonKey = if (savedLesson > 8) 8 else savedLesson
        val offlineTimeStr = alarmPref.getString("time_stunde_$lessonKey", "08:00") ?: "08:00"

        return try {
            val parts = offlineTimeStr.split(":")
            intArrayOf(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            intArrayOf(-1, -1)
        }
    }

    private fun getDayString(date: LocalDate): String? {
        return when (date.dayOfWeek.name.uppercase()) {
            "MONDAY"    -> "montag"
            "TUESDAY"   -> "dienstag"
            "WEDNESDAY" -> "mittwoch"
            "THURSDAY"  -> "donnerstag"
            "FRIDAY"    -> "freitag"
            else        -> null
        }
    }
}
