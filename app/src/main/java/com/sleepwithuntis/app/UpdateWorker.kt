package com.sleepwithuntis.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import android.content.Intent
import android.app.PendingIntent
import androidx.work.Data
import androidx.work.WorkManager

class UpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val alarmPref = applicationContext.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val isInitialScan = inputData.getBoolean("is_initial_scan", false)

        LogManager.log(applicationContext, "--- UpdateWorker gestartet (InitialScan: $isInitialScan) ---")

        if (isInitialScan) {
            runBlocking {
                try {
                    LogManager.log(applicationContext, "Starte UntisSync...")
                    UntisSyncManager(applicationContext).syncNow()
                    LogManager.log(applicationContext, "Sync erfolgreich beendet.")
                } catch (e: Exception) {
                    LogManager.log(applicationContext, "Fehler beim Sync: ${e.message}")
                }
            }
        }

        try {
            val calculator = AlarmCalculator(applicationContext)
            val heute = LocalDate.now()

            if (isInitialScan) {
                findNextSchoolDay(calculator, heute)
            } else {
                LogManager.log(applicationContext, "5-Minuten Check läuft...")
                val jetzt = LocalTime.now()
                // Ignoriere Stunden, die bereits mehr als 30 Min in der Vergangenheit liegen
                val targetTime = calculator.getAlarmTime(heute, jetzt.minusMinutes(30))
                val targetHour = targetTime?.get(0) ?: -1
                val targetMin = targetTime?.get(1) ?: -1

                if (targetHour == -1) {
                    LogManager.log(applicationContext, "Heute kein Unterricht mehr. Suche nächsten Schultag...")
                    findNextSchoolDay(calculator, heute.plusDays(1))
                    return Result.success()
                }

                val alteH = alarmPref.getInt("current_hour", -1)
                val alteM = alarmPref.getInt("current_minute", -1)

                LogManager.log(applicationContext, "Check: Alt=$alteH:$alteM, Neu=$targetHour:$targetMin")

                val newWakeTime = LocalTime.of(targetHour, targetMin)

                if (targetHour == alteH && targetMin == alteM) {
                    LogManager.log(applicationContext, "Zeit unverändert -> Wecker wird ausgelöst.")
                    triggerTaskerBefore()
                    AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
                } else if (newWakeTime.isBefore(jetzt)) {
                    // Sicherheitscheck: Nur sofort wecken, wenn die Zeit erst vor kurzem war (max 30 Min)
                    if (java.time.Duration.between(newWakeTime, jetzt).toMinutes() < 30) {
                        LogManager.log(applicationContext, "Neue Zeit liegt kurz in der Vergangenheit -> Sofort wecken.")
                        saveCurrentAlarm(targetHour, targetMin, false)
                        AlarmReceiver().scheduleFinalAlarm(applicationContext, 0)
                    } else {
                        LogManager.log(applicationContext, "Neue Zeit liegt zu weit in der Vergangenheit ($newWakeTime). Suche nächsten Tag.")
                        findNextSchoolDay(calculator, heute.plusDays(1))
                    }
                } else {
                    LogManager.log(applicationContext, "Zeit hat sich verschoben -> Neuer Scan geplant.")
                    saveCurrentAlarm(targetHour, targetMin, false)
                    AlarmReceiver().scheduleScanFromPrefs(applicationContext)
                }
            }
            return Result.success()

        } catch (e: Exception) {
            LogManager.log(applicationContext, "KRITISCHER FEHLER: ${e.message}")
            if (!isInitialScan) {
                AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
            }
            return Result.failure()
        }
    }

    private fun findNextSchoolDay(calculator: AlarmCalculator, startDate: LocalDate) {
        var targetDate = startDate
        val jetzt = LocalTime.now()

        // Erste Prüfung für den Start-Tag
        var alarmTime = calculator.getAlarmTime(targetDate)

        // Falls wir "heute" suchen, aber der Weckzeitpunkt heute schon vorbei ist -> ab morgen suchen
        if (targetDate == LocalDate.now() && alarmTime != null && alarmTime[0] != -1) {
            val schoolStart = LocalTime.of(alarmTime[0], alarmTime[1])
            if (jetzt.isAfter(schoolStart)) {
                LogManager.log(applicationContext, "Weckzeit für heute ($schoolStart) bereits vorbei. Suche ab morgen.")
                targetDate = targetDate.plusDays(1)
                alarmTime = calculator.getAlarmTime(targetDate)
            }
        }

        // Suche bis zu 7 Tage in die Zukunft
        var searchCount = 0
        while ((alarmTime == null || alarmTime[0] == -1) && searchCount < 7) {
            targetDate = targetDate.plusDays(1)
            alarmTime = calculator.getAlarmTime(targetDate)
            searchCount++
        }

        val targetHour = alarmTime?.get(0) ?: -1
        val targetMin = alarmTime?.get(1) ?: -1
        val isForTomorrow = targetDate.isAfter(LocalDate.now())

        if (targetHour != -1) {
            LogManager.log(applicationContext, "Nächster Alarm gefunden: $targetHour:$targetMin am $targetDate")
            saveCurrentAlarm(targetHour, targetMin, isForTomorrow)
            scheduleSleepReminder(targetHour, targetMin)
            AlarmReceiver().scheduleScanFromPrefs(applicationContext)
        } else {
            LogManager.log(applicationContext, "Kein Unterricht in Sicht. Plane Sicherheits-Scan für morgen.")
            saveCurrentAlarm(-1, -1, false)
            scheduleBackupScan()
        }
    }

    private fun scheduleBackupScan() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 8) // Backup-Scan jeden Morgen um 8
            set(Calendar.MINUTE, 0)
        }
        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_UPDATE_UNTIS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 300, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
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
        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_SLEEP_REMINDER }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 300, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
                val intent = Intent("com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE").setPackage("net.dinglisch.android.taskerm")
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) { }
        }.start()
    }
}