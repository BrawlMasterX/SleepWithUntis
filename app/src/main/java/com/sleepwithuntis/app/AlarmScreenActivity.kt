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

        LogManager.log(this, "AlarmScreenActivity: Wecker-Bildschirm angezeigt.")

        window.statusBarColor = Color.parseColor("#F46614")
        window.navigationBarColor = Color.parseColor("#FF6033")

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

        startAlarmSound()

        val timeTextView: TextView = findViewById(R.id.text_view_alarm_time)
        val dismissButton: MaterialButton = findViewById(R.id.button_dismiss_alarm)
        val snoozeButton: MaterialButton = findViewById(R.id.button_snooze_alarm)

        val clockRunnable = object : Runnable {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
                timeTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockRunnable)

        dismissButton.setOnClickListener {
            LogManager.log(this, "AlarmScreenActivity: Wecker beendet.")
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
                LogManager.log(context ?: return, "AlarmScreenActivity: Bildschirm aus -> Snooze ausgelöst.")
                snoozeAlarm()
            }
        }
    }

    private fun startAlarmSound() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
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
                    val randomUriString = uriSet.random()
                    LogManager.log(this@AlarmScreenActivity, "AlarmScreenActivity: Spiele zufälligen Ton: $randomUriString")
                    setDataSource(this@AlarmScreenActivity, Uri.parse(randomUriString))
                } else {
                    LogManager.log(this@AlarmScreenActivity, "AlarmScreenActivity: Keine Custom-Töne, nutze Standard.")
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
            LogManager.log(this, "AlarmScreenActivity Fehler beim Ton: ${e.message}")
            e.printStackTrace()
            playFallbackSound()
        }
    }

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
        } catch (e: Exception) { 
            LogManager.log(this, "AlarmScreenActivity: KRITISCHER FEHLER - Kein Ton möglich.")
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun snoozeAlarm() {
        LogManager.log(this, "AlarmScreenActivity: Snooze gedrückt.")
        stopAlarmSound()

        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val snoozeMinutes = prefs.getInt("snooze_duration", 5)

        AlarmReceiver().scheduleFinalAlarm(this, snoozeMinutes, true)
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
        LogManager.log(this, "AlarmScreenActivity: Sende Broadcast an Tasker (ALARM_NOW)...")
        Thread {
            try {
                val intent = Intent("com.sleepwithuntis.app.ACTION_ALARM_NOW").apply {
                    setPackage("net.dinglisch.android.taskerm")
                }
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) { 
                LogManager.log(applicationContext, "AlarmScreenActivity Fehler Tasker: ${e.message}")
            }
        }.start()
    }

    private fun checkForAppUpdates() {
        // ... (Update Check Logik bleibt gleich, kann bei Bedarf auch geloggt werden)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_POWER -> {
                snoozeAlarm()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Ignorieren
    }
}
