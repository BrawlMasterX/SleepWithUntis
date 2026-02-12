package com.sleepwithuntis.app

import AlarmCalculator
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
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
        scheduleDailyTask(applicationContext)

        val alarmPref = applicationContext.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // WOCHENENDE-CHECK: Wenn Samstag (7) oder Sonntag (1), dann gar nichts tun.
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return Result.success() // Worker beenden, kein Wecker am Wochenende
        }

        val isInitialScan = inputData.getBoolean("is_initial_scan", false)
        if (isInitialScan) {
            // Hier warten wir blockierend, bis der Sync fertig ist
            runBlocking {
                UntisSyncManager(applicationContext).syncNow()
            }
        }
        val heuteWoche = LocalDate.now().dayOfWeek

        try {
            val calculator = AlarmCalculator(applicationContext)
            // Im UpdateWorker bei isInitialScan:
            val targetDate = if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 21) {
                LocalDate.now().plusDays(1)
            } else {
                LocalDate.now()
            }
            val targetTime = calculator.getAlarmTime(targetDate)
            val targetHour = targetTime?.get(0) ?: -1
            val targetMin = targetTime?.get(1) ?: -1

            // Falls auch offline kein Unterricht ist (z.B. durch den Check in getOfflineSchoolStart)
            if (targetHour == -1) return Result.success()

            if (isInitialScan) {
                saveCurrentAlarm(targetHour, targetMin)
                scheduleSleepReminder(targetHour, targetMin) // <--- Neu hinzufügen
                AlarmReceiver().scheduleScanFromPrefs(applicationContext)
            } else {
                val alteH = alarmPref.getInt("current_hour", -1)
                val alteM = alarmPref.getInt("current_minute", -1)
                val now = Calendar.getInstance()
                val newWakeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMin)
                    set(Calendar.SECOND, 0)
                }

                if (targetHour == alteH && targetMin == alteM) {
                    // 1. Alles wie geplant
                    if (heuteWoche != DayOfWeek.SATURDAY && heuteWoche != DayOfWeek.SUNDAY && targetHour != -1) {
                        hueLightTrigger()
                        triggerTaskerBefore()
                        AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
                    }
                } else if (newWakeCal.before(now)) {
                    if (heuteWoche != DayOfWeek.SATURDAY && heuteWoche != DayOfWeek.SUNDAY && targetHour != -1) {
                        // 2. Die NEUE Weckzeit liegt in der Vergangenheit (zu spät!)
                        saveCurrentAlarm(targetHour, targetMin)
                        AlarmReceiver().scheduleFinalAlarm(applicationContext, 0) // Sofort wecken!
                    }
                } else {
                    if (heuteWoche != DayOfWeek.SATURDAY && heuteWoche != DayOfWeek.SUNDAY && targetHour != -1) {
                        // 3. Die Zeit hat sich nach hinten verschoben (z.B. von 7:00 auf 8:00)
                        saveCurrentAlarm(targetHour, targetMin)
                        AlarmReceiver().scheduleScanFromPrefs(applicationContext) // Neuen Scan in der Zukunft planen
                    }
                }
            }


            return Result.success()

        } catch (e: Exception) {
            if (!isInitialScan && heuteWoche != DayOfWeek.SATURDAY && heuteWoche != DayOfWeek.SUNDAY) {
                // Nur im Notfall wecken, wenn wir uns bereits im 5-Min-Check befinden
                hueLightTrigger()
                triggerTaskerBefore()
                AlarmReceiver().scheduleFinalAlarm(applicationContext, 5)
            }
            return Result.failure()
        }
    }
    fun scheduleDailyTask(context: Context) {
        val now = Calendar.getInstance()
        val executionTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // Falls es heute schon nach 18 Uhr ist, plane für morgen 18 Uhr
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = executionTime.timeInMillis - now.timeInMillis

        val data = Data.Builder()
            .putBoolean("is_initial_scan", true)
            .build()

        val dailyRequest = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyUntisCheck", // Name geändert
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }
    private fun scheduleSleepReminder(h: Int, m: Int) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            // 8 Stunden zurückrechnen
            add(Calendar.HOUR_OF_DAY, -8)
        }

        // Wenn die berechnete Schlafenszeit schon in der Vergangenheit liegt (heute), nichts tun
        if (calendar.before(Calendar.getInstance())) return

        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SLEEP_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            300, // Eigener RequestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Wir nutzen hier set (nicht exact), da es auf ein paar Minuten nicht ankommt
        // und das System so Akku spart
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }


    private fun saveCurrentAlarm(h: Int, m: Int) {
        applicationContext.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
            putInt("current_hour", h)
            putInt("current_minute", m)
            apply()
        }
    }

    private fun hueLightTrigger() {
        val userPref = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val u = userPref.getString("username", "")
        val s = userPref.getString("school", "")
        val itemName = when {
            u == "KolNoa09_A28" && s == "egwoerth" -> "LichtweckerNoah"
            u == "KolbFli" && s == "egwoerth" -> "LichtweckerFlinn"
            else -> return
        }
        Thread {
            try {
                val conn = URL("http://openhab.local:8080/rest/items/$itemName").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "text/plain")
                conn.outputStream.use { it.write("ON".toByteArray()) }
                conn.responseCode
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
    private fun triggerTaskerBefore() {
        Thread {
            try {
                val intent = Intent("com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE").apply {
                    // Wir schränken es auf Tasker ein, damit das System effizient bleibt
                    setPackage("net.dinglisch.android.taskerm")
                }
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}