package com.sleepwithuntis.app

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val ALARM_NOTIFICATION_ID = 1
        const val SCAN_REQUEST_CODE = 100
        const val TRIGGER_REQUEST_CODE = 200

        const val ACTION_UPDATE_UNTIS = "com.sleepwithuntis.ACTION_UPDATE_UNTIS"
        const val ACTION_TRIGGER_ALARM = "com.sleepwithuntis.ACTION_TRIGGER_ALARM"
        const val ACTION_SLEEP_REMINDER = "com.sleepwithuntis.ACTION_SLEEP_REMINDER"
        const val SLEEP_NOTIFICATION_ID = 3
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action
        val pref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Alarme nach Neustart wiederherstellen
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleScanFromPrefs(context)
            return
        }

        // Sicherheitscheck: Nur weitermachen, wenn der Wecker aktiv ist
        if (!pref.getBoolean("alarm_active", false)) return

        when (action) {
            ACTION_UPDATE_UNTIS -> {
                // Den UpdateWorker für den 5-Minuten-Vergleich starten
                val data = Data.Builder()
                    .putBoolean("is_initial_scan", false) // Kein 0-Uhr Scan, sondern der Check vor dem Wecken
                    .build()

                val updateRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(context).enqueue(updateRequest)
            }

            ACTION_TRIGGER_ALARM -> {
                // Nur wenn es KEIN Snooze ist, setzen wir die Flag auf true
                triggerAlarm(context)
            }
            ACTION_SLEEP_REMINDER -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                // PRÜFUNG: Ist der Bildschirm an?
                if (powerManager.isInteractive) {
                    showSleepNotification(context)
                }
            }
        }
    }

    fun scheduleScanFromPrefs(context: Context) {
        val pref = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val h = pref.getInt("current_hour", -1)
        val m = pref.getInt("current_minute", -1)

        if (h == -1) return // Kein Alarm gesetzt

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -5) // Genau 5 Minuten vor der Weckzeit scannen
        }

        // Falls die Zeit für heute schon vorbei ist -> Plan für morgen
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_UPDATE_UNTIS }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SCAN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }


    // Füge isSnooze als Parameter hinzu (Standardwert ist false)
    fun scheduleFinalAlarm(context: Context, minutesFromNow: Int, isSnooze: Boolean = false) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutesFromNow)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            // Hier geben wir die Info mit, ob es ein Snooze ist
            putExtra("is_snooze", isSnooze)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TRIGGER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun setExactAlarm(am: AlarmManager, time: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }

    private fun triggerAlarm(context: Context) { {
        context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
            putBoolean("last_set_time",true )
                apply()
            }
        }
        val fullScreenIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Wenn Overlay-Rechte da sind -> Activity sofort öffnen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            context.startActivity(fullScreenIntent)
        } else {
            // Fallback: Benachrichtigung mit FullScreenIntent
            createNotificationChannel(context)
            val pending = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Wecker")
                .setContentText("Zeit aufzustehen!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pending, true)
                .setAutoCancel(true)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ALARM_NOTIFICATION_ID, notification)
        }
    }
    @SuppressLint("ServiceCast")
    private fun showSleepNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context) // Nutzt deine vorhandene Channel-Funktion

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentTitle("Ab ins Bett!")
            .setContentText("Dein Wecker geht in 8 Stunden. Zeit zu schlafen.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(SLEEP_NOTIFICATION_ID, notification)
    }


    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wecker Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // Ton kommt von der Activity
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}