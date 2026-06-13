package com.sleepwithuntis.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Statusleiste orange machen
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")

        setContentView(R.layout.activity_settings)

        // Toolbar Setup für Home-Button
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        toolbar.setNavigationOnClickListener {
            // Gehe zur MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        val navigationIcon = toolbar.navigationIcon
        if (navigationIcon != null) {
            navigationIcon.setTint(Color.parseColor("#BF360C"))
        }

        // Karten finden
        val cardLoginData = findViewById<MaterialCardView>(R.id.card_login_data)
        val cardEarlyMinutes = findViewById<MaterialCardView>(R.id.card_early_minutes)
        val cardAlarmSetup = findViewById<MaterialCardView>(R.id.card_alarm_setup)
        val cardLogs = findViewById<MaterialCardView>(R.id.card_logs)
        val cardCopyright = findViewById<MaterialCardView>(R.id.card_copyright)


        // Navigationen
        cardLoginData.setOnClickListener {
            startActivity(Intent(this, UserLoginActivity::class.java))
        }

        cardEarlyMinutes.setOnClickListener {
            startActivity(Intent(this, WakeUpTimesActivity::class.java))
        }

        cardAlarmSetup.setOnClickListener {
            startActivity(Intent(this, AlarmSetupActivity::class.java))
        }

        cardLogs.setOnClickListener {
            showLogDialog()
        }

        cardCopyright.setOnClickListener {
            startActivity(Intent(this, CopyrightActivity::class.java))
        }
    }

    private fun showLogDialog() {
        val logs = LogManager.getLogs(this)
        
        val textView = TextView(this).apply {
            text = logs
            setPadding(40, 40, 40, 40)
            textSize = 12f
        }

        val scroll = android.widget.ScrollView(this).apply {
            addView(textView)
        }

        AlertDialog.Builder(this)
            .setTitle("Logbuch")
            .setView(scroll)
            .setPositiveButton("OK", null)
            .setNeutralButton("Löschen") { _, _ ->
                LogManager.clearLogs(this)
                Toast.makeText(this, "Logs gelöscht", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Kopieren") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("App Logs", logs)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "In Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
