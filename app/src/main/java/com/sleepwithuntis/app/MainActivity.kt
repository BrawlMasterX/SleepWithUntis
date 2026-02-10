package com.sleepwithuntis.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import AlarmCalculator
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.os.PowerManager
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var timeDisplay: TextView
    private lateinit var dateDisplay: TextView
    private lateinit var alarmSwitch: SwitchMaterial
    private lateinit var settingsButton: ImageButton
    private lateinit var schrifttext: TextView
    private lateinit var webuntis_mobile: com.google.android.material.button.MaterialButton




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasUserData()) {
            redirectToUserActivity()
            return
        }

        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleDailyTask(context = this)
        initUI()
        setupPermissions()
        setupListeners()

        // 1. SCHRITT: Sofort anzeigen, was wir bereits wissen
        loadInitialDataFromPrefs()

        // 2. SCHRITT: Im Hintergrund nach Updates suchen (Untis oder Offline-Fallback)
        checkForUpdate()
    }

    private fun initUI() {
        timeDisplay = findViewById(R.id.text_view_time_display)
        dateDisplay = findViewById(R.id.text_view_date_display)
        alarmSwitch = findViewById(R.id.switch_alarm)
        settingsButton = findViewById(R.id.settings_button)
        schrifttext= findViewById(R.id.weckzeit_title)
        webuntis_mobile= findViewById(R.id.btn_open_untis)


        alarmSwitch.isChecked = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).getBoolean("alarm_active", false)
    }
    private fun putDayToWakeUp() {
        val pref = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // 1. Hole das Flag (ist es für morgen gesetzt?)
        val isForTomorrow = pref.getBoolean("last_set_time", false)

        // 2. Erstelle das Datum und weise das Ergebnis von plusDays zu!
        var targetDate = LocalDate.now()
        if (isForTomorrow) {
            targetDate = targetDate.plusDays(1) // WICHTIG: Zuweisung nutzen
        }

        val formatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN)
        dateDisplay.text = targetDate.format(formatter)
    }


    private fun loadInitialDataFromPrefs() {
        val pref = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val h = pref.getInt("current_hour", -1)
        val m = pref.getInt("current_minute", -1)
        putDayToWakeUp()
        updateUi(h, m)
    }


    private fun checkForUpdate() {
        val calculator = AlarmCalculator(this) // 'this' ist der Context
        lifecycleScope.launch(Dispatchers.IO) {
            val jetzt = LocalTime.now()
            val heute = LocalDate.now()
            val morgen = heute.plusDays(1)

                // 1. Versuch für das aktuelle Datum
            var targetTime = calculator.getAlarmTime(heute)
            var firstHourTime = targetTime?.let { LocalTime.of(10, 0) }

            if (targetTime?.get(0) != -1) {
                firstHourTime = targetTime?.let { LocalTime.of(it[0], targetTime[1]) }
            }

            if (jetzt.isAfter(firstHourTime)) {
                getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
                    putBoolean("last_set_time", true)
                    apply()
                }
                targetTime = calculator.getAlarmTime(morgen)
            } else {
                // Zeit liegt noch in der Zukunft (heute)
                getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
                    putBoolean("last_set_time", false)
                    apply()
                }
            }
            targetTime?.let { saveAndRefresh(targetTime[0], it[1]) }
            runBlocking {
                UntisSyncManager(applicationContext).syncNow()
            }

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

    private suspend fun saveAndRefresh(h: Int, m: Int) {
        getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
            putInt("current_hour", h)
            putInt("current_minute", m)
            apply()
        }

        withContext(Dispatchers.Main) {
            updateUi(h, m)
            putDayToWakeUp()

            if (alarmSwitch.isChecked) {
                AlarmReceiver().scheduleScanFromPrefs(this@MainActivity)
            }
        }
    }

    private fun updateUi(h: Int, m: Int) {
        if (h != -1 && m != -1) {
            schrifttext.text = "WECKEN UM"
            timeDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        } else {
            schrifttext.text = "DU HAST"
            timeDisplay.text = "FREI"
        }
    }

    private fun setupListeners() {
        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().putBoolean("alarm_active", isChecked).apply()
            if (isChecked) {
                AlarmReceiver().scheduleScanFromPrefs(this)
                Toast.makeText(this, "Wecker aktiviert.", Toast.LENGTH_SHORT).show()
            } else {
                cancelAlarm()
                Toast.makeText(this, "Wecker deaktiviert.", Toast.LENGTH_SHORT).show()
            }
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        timeDisplay.setOnClickListener {
            showSleepDurationToast()
        }
        webuntis_mobile.setOnClickListener {
            openUntisApp(this)
        }
    }

    private fun cancelAlarm() {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pScan = PendingIntent.getBroadcast(this, AlarmReceiver.SCAN_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pTrigger = PendingIntent.getBroadcast(this, AlarmReceiver.TRIGGER_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pScan)
        alarmManager.cancel(pTrigger)
    }

    private fun showSleepDurationToast() {
        val pref = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val h = pref.getInt("current_hour", -1)
        val m = pref.getInt("current_minute", -1)
        if (h == -1) return

        val now = Calendar.getInstance()
        val wakeUpTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }
        if (wakeUpTime.before(now)) wakeUpTime.add(Calendar.DATE, 1)

        val diffMs = wakeUpTime.timeInMillis - now.timeInMillis
        val hours = diffMs / (1000 * 60 * 60)
        val minutes = (diffMs / (1000 * 60)) % 60
        Toast.makeText(this, "Noch ${hours}h ${minutes}min Schlaf", Toast.LENGTH_SHORT).show()
    }

    private fun hasUserData(): Boolean = !getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("server", "").isNullOrEmpty()
    private fun redirectToUserActivity() { startActivity(Intent(this, UserLoginActivity::class.java)); finish() }
    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
    fun openUntisApp(context: Context) {
        webuntis_mobile.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                webuntis_mobile.animate().scaleX(1f).scaleY(1f).setDuration(100).start()

                // 2. Die eigentliche Logik zum App-Öffnen
                val packageName = "com.grupet.web.app"
                val intent = packageManager.getLaunchIntentForPackage(packageName)

                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Untis Mobile nicht installiert", Toast.LENGTH_SHORT).show()
                }
            }.start()

    }
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
}