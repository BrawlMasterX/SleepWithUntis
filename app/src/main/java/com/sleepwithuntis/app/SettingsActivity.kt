package com.sleepwithuntis.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
            // Gehe zur MainActivity (Passe den Namen ggf. an)
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
        val cardCopyright = findViewById<MaterialCardView>(R.id.card_copyright)


        // Navigationen
        cardLoginData.setOnClickListener {
            startActivity(Intent(this, UserLoginActivity::class.java))
        }


        cardEarlyMinutes.setOnClickListener {
            startActivity(Intent(this, WakeUpTimesActivity::class.java))
        }

        // Logik für das neue Alarm-Setup Feld
        cardAlarmSetup.setOnClickListener {
            startActivity(Intent(this, AlarmSetupActivity::class.java))
        }
        cardCopyright.setOnClickListener {
            startActivity(Intent(this, CopyrightActivity::class.java))
        }

    }
}