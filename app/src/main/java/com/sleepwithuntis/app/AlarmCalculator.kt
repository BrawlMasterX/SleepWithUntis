import android.content.Context
import webuntisjava.UntisClient
import java.time.LocalDate
import java.time.LocalTime
class AlarmCalculator(private val context: Context) {

    // Rückgabetyp IntArray: [Stunde, Minute]
    fun getAlarmTime(targetDate: LocalDate): IntArray? {

        val alarmPrefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val useAbsoluteTime = alarmPrefs.getBoolean("use_absolute_time", false)

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
                        val parts = firstHour?.split(":")
                        val h = parts?.get(0)?.toInt()
                        val m = parts?.get(1)?.toInt()
                        if (h != null && m != null) {
                            return intArrayOf(h, m)
                        }else {
                            return intArrayOf(-1,-1)
                        }

                    } else {
                        return intArrayOf(-1,-1)
                    }
                }

            } catch (e: Exception) {
                showToast("Kein Internet")
                return (useOfflineFallbackAbsolute(targetDate))
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
                        return intArrayOf(alarmTime.hour, alarmTime.minute)
                    }else {
                        return intArrayOf(-1,-1)
                    }

                }
            } catch (e: Exception) {
                showToast("Kein Internet")
                return (useOfflineFallbackEarly(earlyMinutes, targetDate))
            }
        }
        showToast("Schalter in Konfiguration nicht gesetzt!")
        return intArrayOf(-1,-1)
    }
    // In deiner getAlarmTime oder showToast Funktion
    private fun showToast(text: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private fun useOfflineFallbackEarly(early: Long, targetDate: LocalDate): IntArray {
        val alarmPref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Wochentag ermitteln
        val dayString = when (targetDate.dayOfWeek.name.uppercase()) {
            "MONDAY"    -> "montag"
            "TUESDAY"   -> "dienstag"
            "WEDNESDAY" -> "mittwoch"
            "THURSDAY"  -> "donnerstag"
            "FRIDAY"    -> "freitag"
            else        -> return intArrayOf(-1, -1) // Am Wochenende kein Alarm
        }
        val offlineTimeStr = alarmPref.getString("wake_up_time_$dayString", "08:05") ?: "08:05"
        val parts = offlineTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 5
        if (h != -1) {
            val firstHourTime = LocalTime.of(h, m)
            val alarmTime = firstHourTime.minusMinutes(early)
            return intArrayOf(alarmTime.hour, alarmTime.minute)
        }else {
            return intArrayOf(-1,-1)
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
            else        -> return intArrayOf(-1, -1) // Am Wochenende kein Alarm
        }

        val savedLesson = alarmPref.getInt("wake_up_lesson_$dayString", 1)
        val lessonKey = if (savedLesson > 8) 8 else savedLesson

        val offlineTimeStr = alarmPref.getString("time_stunde_$lessonKey", "08:00") ?: "08:00"


        return try {
            val parts = offlineTimeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            intArrayOf(h, m)
        } catch (e: Exception) {
            intArrayOf(-1, -1)
        }
    }
}