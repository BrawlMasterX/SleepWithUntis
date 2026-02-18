package com.sleepwithuntis.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CopyrightActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Statusleiste orange machen
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")

        setContentView(R.layout.activity_copyright)

        // Toolbar Setup für Home-Button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_credits)
        toolbar.setNavigationOnClickListener {
            // Gehe zur MainActivity (Passe den Namen ggf. an)
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        val navigationIcon = toolbar.navigationIcon
        if (navigationIcon != null) {
            navigationIcon.setTint(Color.parseColor("#BF360C"))
        }

    }
}