package com.sleepwithuntis.app

import android.content.Context
import webuntisjava.UntisClient
import java.time.LocalDate
import java.time.LocalTime

class AlarmCalculator(private val context: Context) {

    fun getAlarmTime(targetDate: LocalDate): IntArray? {
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
                    var firstLesson = client.getFirstLessonForDate(targetDate, true)
                    client.logout()
                    
                    if (firstLesson != -1) {
                        if (firstLesson > 8) { firstLesson = 8 }
                        val firstHour = alarmPrefs.getString("time_stunde_$firstLesson", "08:00")
                        LogManager.log(context, "AlarmCalculator: Erste Stunde ist #$firstLesson. Zeit aus Settings: $firstHour")
                        
                        val parts = firstHour?.split(":")
                        val h = parts?.get(0)?.toIntOrNull()
                        val m = parts?.get(1)?.toIntOrNull()
                        
                        if (h != null && m != null) {
                            return intArrayOf(h, m)
                        } else {
                            LogManager.log(context, "AlarmCalculator Fehler: Zeitformat ungültig: $firstHour")
                            return intArrayOf(-1, -1)
                        }
                    } else {
                        LogManager.log(context, "AlarmCalculator: Keine Stunden für $targetDate gefunden.")
                        return intArrayOf(-1, -1)
                    }
                } else {
                    LogManager.log(context, "AlarmCalculator: Untis Login fehlgeschlagen.")
                    return intArrayOf(-1, -1)
                }
            } catch (e: Exception) {
                LogManager.log(context, "AlarmCalculator: Fehler bei Online-Abfrage (Absolut): ${e.message}. Nutze Fallback.")
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
                        LogManager.log(context, "AlarmCalculator: Unterrichtsbeginn $firstHourTime. Wecken um $alarmTime (-$earlyMinutes Min)")
                        return intArrayOf(alarmTime.hour, alarmTime.minute)
                    } else {
                        LogManager.log(context, "AlarmCalculator: Keine Stunden für $targetDate gefunden.")
                        return intArrayOf(-1, -1)
                    }
                } else {
                    LogManager.log(context, "AlarmCalculator: Untis Login fehlgeschlagen.")
                    return intArrayOf(-1, -1)
                }
            } catch (e: Exception) {
                LogManager.log(context, "AlarmCalculator: Fehler bei Online-Abfrage (Früher): ${e.message}. Nutze Fallback.")
                return useOfflineFallbackEarly(earlyMinutes, targetDate)
            }
        }
    }

    private fun useOfflineFallbackEarly(early: Long, targetDate: LocalDate): IntArray {
        val alarmPref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        val dayString = when (targetDate.dayOfWeek.name.uppercase()) {
            "MONDAY"    -> "montag"
            "TUESDAY"   -> "dienstag"
            "WEDNESDAY" -> "mittwoch"
            "THURSDAY"  -> "donnerstag"
            "FRIDAY"    -> "freitag"
            else        -> {
                LogManager.log(context, "AlarmCalculator Fallback: Wochenende ($targetDate), kein Alarm.")
                return intArrayOf(-1, -1)
            }
        }
        
        val offlineTimeStr = alarmPref.getString("wake_up_time_$dayString", "08:05") ?: "08:05"
        LogManager.log(context, "AlarmCalculator Fallback (Früher): Gespeicherte Zeit für $dayString ist $offlineTimeStr")
        
        val parts = offlineTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 5
        
        if (h != -1) {
            val firstHourTime = LocalTime.of(h, m)
            val alarmTime = firstHourTime.minusMinutes(early)
            return intArrayOf(alarmTime.hour, alarmTime.minute)
        } else {
            return intArrayOf(-1, -1)
        }
    }

    private fun useOfflineFallbackAbsolute(targetDate: LocalDate): IntArray {
        val alarmPref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        val dayString = when (targetDate.dayOfWeek.name.uppercase()) {
            "MONDAY"    -> "montag"
            "TUESDAY"   -> "dienstag"
            "WEDNESDAY" -> "mittwoch"
            "THURSDAY"  -> "donnerstag"
            "FRIDAY"    -> "freitag"
            else        -> {
                LogManager.log(context, "AlarmCalculator Fallback: Wochenende ($targetDate), kein Alarm.")
                return intArrayOf(-1, -1)
            }
        }

        val savedLesson = alarmPref.getInt("wake_up_lesson_$dayString", 1)
        val lessonKey = if (savedLesson > 8) 8 else savedLesson
        val offlineTimeStr = alarmPref.getString("time_stunde_$lessonKey", "08:00") ?: "08:00"

        LogManager.log(context, "AlarmCalculator Fallback (Absolut): Gespeicherte Stunde für $dayString ist #$lessonKey ($offlineTimeStr)")

        return try {
            val parts = offlineTimeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            intArrayOf(h, m)
        } catch (e: Exception) {
            LogManager.log(context, "AlarmCalculator Fallback Fehler: Zeitformat ungültig: $offlineTimeStr")
            intArrayOf(-1, -1)
        }
    }
}
