package com.sleepwithuntis.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat

class AlarmScreenActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Statusbar und Navigationbar Farben (passend zum Alarm-Design)
        window.statusBarColor = Color.parseColor("#F46614")
        window.navigationBarColor = Color.parseColor("#FF6033")

        // Wecker über dem Sperrbildschirm anzeigen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        setContentView(R.layout.dialog_alarm_screen)
        triggerTaskerAlarm()

        // Ton starten (berücksichtigt jetzt Reset und neue Formate)
        startAlarmSound()

        val timeTextView: TextView = findViewById(R.id.text_view_alarm_time)
        val dismissButton: MaterialButton = findViewById(R.id.button_dismiss_alarm)
        val snoozeButton: MaterialButton = findViewById(R.id.button_snooze_alarm)

        // Live-Uhr im Wecker-Bildschirm
        val clockRunnable = object : Runnable {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
                timeTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockRunnable)

        dismissButton.setOnClickListener {
            stopAlarmSound()
            finishAndRemoveTask()
        }

        snoozeButton.setOnClickListener {
            snoozeAlarm()
        }
    }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                snoozeAlarm()
            }
        }
    }

    private fun startAlarmSound() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        // Liste laden und in eine normale Liste umwandeln
        val uriSet = prefs.getStringSet("custom_mp3_uris", null)?.toList()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                if (!uriSet.isNullOrEmpty()) {
                    // ZUFALL: Wähle eine zufällige URI aus der Liste
                    val randomUriString = uriSet.random()
                    setDataSource(this@AlarmScreenActivity, Uri.parse(randomUriString))
                } else {
                    // FALLBACK: Standard-Ton aus raw oder System
                    try {
                        val afd = resources.openRawResourceFd(R.raw.alarm_sound)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                        afd.close()
                    } catch (e: Exception) {
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        setDataSource(this@AlarmScreenActivity, alarmUri)
                    }
                }

                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playFallbackSound() // Notfall-Funktion (System-Standard)
        }
    }

    // Hilfsfunktion für den Notfall (System-Standardton)
    private fun playFallbackSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setDataSource(this@AlarmScreenActivity, alarmUri)
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun snoozeAlarm() {
        stopAlarmSound()

        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val snoozeMinutes = prefs.getInt("snooze_duration", 5)

        // Alarm neu planen über deinen Receiver
        AlarmReceiver().scheduleFinalAlarm(this, snoozeMinutes)

        finishAndRemoveTask()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {}
        stopAlarmSound()
        super.onDestroy()
    }

    private fun triggerTaskerAlarm() {
        Thread {
            try {
                val intent = Intent("com.sleepwithuntis.app.ACTION_ALARM_NOW").apply {
                    //schränken es auf Tasker ein
                    setPackage("net.dinglisch.android.taskerm")
                }
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun checkForAppUpdates() {
        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        } ?: "1.0.0"
        val url = "https://api.github.com/repos/BrawlMasterX/SleepWithUntis/releases/latest"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // In der lifecycleScope.launch(Dispatchers.IO) Schleife:

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SleepWithUntis-App") // WICHTIG: GitHub braucht das!
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    if (jsonData != null) {
                        val jsonObject = JSONObject(jsonData)
                        val latestVersion = jsonObject.getString("tag_name").replace("v", "") // Entfernt 'v' falls vorhanden
                        val downloadUrl = jsonObject.getString("html_url")

                        if (isNewerVersion(currentVersion, latestVersion)) {
                            withContext(Dispatchers.Main) {
                                sendUpdateNotification(latestVersion, downloadUrl)
                            }
                        }
                    }
                } else {
                    // Logge den Fehler, falls die API z.B. 404 oder 403 zurückgibt
                    println("GitHub API Error: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace() // Fehler beim Check (z.B. kein Internet) einfach ignorieren
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        // Entferne 'v' Präfixe und splitte bei den Punkten (z.B. "1.2.3" -> ["1", "2", "3"])
        val curParts = current.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        val latParts = latest.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }

        // Ermittle die Länge der längsten Versionsnummer
        val maxLength = maxOf(curParts.size, latParts.size)

        for (i in 0 until maxLength) {
            val c = curParts.getOrElse(i) { 0 } // Falls Teil fehlt (z.B. bei "1.2"), nimm 0
            val l = latParts.getOrElse(i) { 0 }

            if (l > c) return true  // GitHub Version ist an dieser Stelle höher -> Update!
            if (c > l) return false // Lokale Version ist höher (Entwickler-Modus) -> Kein Update
        }

        return false // Versionen sind exakt gleich
    }
    private fun sendUpdateNotification(version: String, downloadUrl: String) {
        val channelId = "update_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // Standard Download-Icon
            .setContentTitle("Update verfügbar!")
            .setContentText("Version $version ist da. Tippe zum Herunterladen von GitHub.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Öffnet den Link
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_POWER -> {
                Toast.makeText(this, "Alarm Snooze", Toast.LENGTH_SHORT).show()
                snoozeAlarm()
                true // Event als "verarbeitet" markieren
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Verhindert das Schließen über die Zurück-Taste (Nutzer muss Snooze oder Dismiss drücken)
    }
}
