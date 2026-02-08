package com.sleepwithuntis.app

import android.content.Context
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

        setContentView(R.layout.dialog_alarm_screen)

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
        stopAlarmSound()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Verhindert das Schließen über die Zurück-Taste (Nutzer muss Snooze oder Dismiss drücken)
    }
}