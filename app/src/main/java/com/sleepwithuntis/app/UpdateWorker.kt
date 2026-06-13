package com.sleepwithuntis.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import android.content.Intent
import android.app.PendingIntent
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class UpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val alarmPref = applicationContext.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val isInitialScan = inputData.getBoolean("is_initial_scan", false)

        if (isInitialScan) {
            runBlocking {
                UntisSyncManager(applicationContext).syncNow()
            }
        }

        try {
            val calculator = AlarmCalculator(applicationContext)
            val jetzt = LocalTime.now()
            val heute = LocalDate.now()

            if (isInitialScan) {
                // Bestimme das Zieldatum für den nächsten Schultag
                var targetDate = heute
                var alarmTime = calculator.getAlarmTime(heute)

                // Wann ist der heutige Schultag "vorbei"?
                val schoolStartHeute = if (alarmTime != null && alarmTime[0] != -1) {
                    LocalTime.of(alarmTime[0], alarmTime[1])
                } else {
                    LocalTime.of(10, 0)
                }

                if (jetzt.isAfter(schoolStartHeute)) {
                    targetDate = heute.plusDays(1)
                    alarmTime = calculator.getAlarmTime(targetDate)
                }

                // Nächsten Schultag suchen (max 7 Tage)
                var searchCount = 0
                while ((alarmTime == null || alarmTime[0] == -1) && searchCount < 7) {
                    targetDate = targetDate.plusDays(1)
                    alarmTime = calculator.getAlarmTime(targetDate)
                    searchCount++
                }

                val targetHour = alarmTime?.get(0) ?: -1
                val targetMin = alarmTime?.get(1) ?: -1
                val isForTomorrow = targetDate.isAfter(heute)

                saveCurrentAlarm(targetHour, targetMin, isForTomorrow)
                
                if (targetHour != -1) {
                    scheduleSleepReminder(targetHour, targetMin)
                    AlarmReceiver().scheduleScanFromPrefs(applicationContext)
                }
            } else {
                // 5-Minuten Check vor dem Wecken
                // Wir scannen IMMER den aktuellen Tag (heute), da wir ja gerade geweckt werden wollen
                val targetTime = calculator.getAlarmTime(heute)
                val targetHour = targetTime?.get(0) ?: -1
                val targetMin = targetTime?.get(1) ?: -1

                if (targetHour == -1) {
                    // Falls heute doch kein Unterricht ist (kurzfristige Änderung)
                    saveCurrentAlarm(-1, -1, false)
                    return Result.success()
                }

                val alteH = alarmPref.getInt("current_hour", -1)
                val alteM = alarmPref.getInt("current_minute", -1)
                
                val nowCal = Calendar.getInstance()
                val newWakeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMin)
                    set(Calendar.SECOND, 0)
                }

                if (targetHour == alteH && targetMin == alteM) {
                    triggerTaskerBefore()
                    AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
                } else if (newWakeCal.before(nowCal)) {
                    // Neue Weckzeit liegt in der Vergangenheit -> Sofort wecken
                    saveCurrentAlarm(targetHour, targetMin, false)
                    AlarmReceiver().scheduleFinalAlarm(applicationContext, 0)
                } else {
                    // Weckzeit hat sich nach hinten verschoben
                    saveCurrentAlarm(targetHour, targetMin, false)
                    AlarmReceiver().scheduleScanFromPrefs(applicationContext)
                }
            }

            return Result.success()

        } catch (e: Exception) {
            if (!isInitialScan) {
                // Im Fehlerfall trotzdem wecken (Sicherheit geht vor)
                triggerTaskerBefore()
                AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
            }
            return Result.failure()
        }
    }

    private fun scheduleSleepReminder(h: Int, m: Int) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            add(Calendar.HOUR_OF_DAY, -8)
        }

        if (calendar.before(Calendar.getInstance())) return

        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SLEEP_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun saveCurrentAlarm(h: Int, m: Int, isForTomorrow: Boolean) {
        applicationContext.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
            putInt("current_hour", h)
            putInt("current_minute", m)
            putBoolean("last_set_time", isForTomorrow)
            apply()
        }
    }

    private fun triggerTaskerBefore() {
        Thread {
            try {
                val intent = Intent("com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE").apply {
                    setPackage("net.dinglisch.android.taskerm")
                }
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
