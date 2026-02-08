package com.sleepwithuntis.app

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class WakeUpTimesActivity : AppCompatActivity() {

    private lateinit var etGlobalMinutes: EditText
    private lateinit var timeViews: MutableList<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up_times)
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")

        val switchMode = findViewById<SwitchMaterial>(R.id.switch_mode)
        val layoutMinutes = findViewById<View>(R.id.layout_state_minutes)
        val layoutHours = findViewById<View>(R.id.layout_state_hours)
        etGlobalMinutes = findViewById(R.id.et_global_minutes)
        timeViews = mutableListOf()

        val pref = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Toolbar mit Back Button Setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar_early)
        toolbar.setNavigationOnClickListener {
            saveEverything() // Alles speichern beim Verlassen
            finish()
        }
        toolbar.navigationIcon?.setTint(Color.parseColor("#BF360C"))

        // Initialen Modus laden
        val isTimeMode = pref.getBoolean("use_absolute_time", false)
        switchMode.isChecked = isTimeMode

        fun toggleUI(timeMode: Boolean) {
            if (timeMode) {
                layoutMinutes.visibility = View.GONE
                layoutHours.visibility = View.VISIBLE
            } else {
                layoutMinutes.visibility = View.VISIBLE
                layoutHours.visibility = View.GONE
            }
        }
        toggleUI(isTimeMode)

        switchMode.setOnCheckedChangeListener { _, isChecked ->
            toggleUI(isChecked)
            // Auch beim Umschalten speichern wir den Modus sofort
            pref.edit().putBoolean("use_absolute_time", isChecked).apply()
        }

        // Minuten laden
        etGlobalMinutes.setText(pref.getInt("early_minutes", 30).toString())

        // Stunden-Liste initialisieren (1-8)
        for (i in 1..8) {
            val id = resources.getIdentifier("tv_weckzeit_$i", "id", packageName)
            val tv = findViewById<TextView>(id)
            if (tv != null) {
                tv.text = pref.getString("time_stunde_$i", "08:00")
                tv.setOnClickListener {
                    val timeParts = tv.text.toString().split(":")
                    val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 7
                    val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                    TimePickerDialog(this, R.style.OrangeTimePickerTheme, { _, selH, selM ->
                        tv.text = String.format("%02d:%02d", selH, selM)
                    }, h, m, true).show()
                }
                timeViews.add(tv)
            }
        }
    }

    // Diese Funktion speichert ALLES gleichzeitig
    private fun saveEverything() {
        val pref = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val editor = pref.edit()

        // 1. Das Minuten-Feld speichern
        val mins = etGlobalMinutes.text.toString().toIntOrNull() ?: 30
        editor.putInt("early_minutes", mins)

        timeViews.forEachIndexed { index, textView ->
            val stunde = index + 1
            editor.putString("time_stunde_$stunde", textView.text.toString())
        }

        editor.apply()
    }

    override fun onBackPressed() {
        saveEverything() // Speichert auch beim System-Zurück-Button
        super.onBackPressed()
    }
}