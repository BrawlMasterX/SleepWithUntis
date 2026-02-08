package com.sleepwithuntis.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import android.net.Uri
import android.provider.OpenableColumns

class AlarmSetupActivity : AppCompatActivity() {

    private val audioMimeTypes = arrayOf("audio/*", "application/ogg")

    // Mehrfachauswahl-Launcher
    private val pickMp3Launcher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

            val uriStrings = uris.map { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            }.toSet()

            // WICHTIG: Speichern als Set unter "custom_mp3_uris"
            prefs.edit().putStringSet("custom_mp3_uris", uriStrings).apply()

            updateUi()
            if (uris.size == 1) {
                Toast.makeText(this, "Soundtrack ausgewählt", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "${uris.size} Soundtracks ausgewählt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_setup)
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")


        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Toolbar Setup
        findViewById<Toolbar>(R.id.toolbar_alarm_setup).setNavigationOnClickListener { finish() }

        // Snooze Slider
        val slider = findViewById<Slider>(R.id.slider_snooze)
        slider.value = prefs.getInt("snooze_duration", 5).toFloat()
        slider.addOnChangeListener { _, value, _ ->
            prefs.edit().putInt("snooze_duration", value.toInt()).apply()
        }

        // Auswahl-Button
        findViewById<MaterialButton>(R.id.btn_select_mp3).setOnClickListener {
            pickMp3Launcher.launch(audioMimeTypes)
        }

        // RESET-BUTTON: Löscht die Liste komplett
        findViewById<ImageButton>(R.id.btn_reset_audio).setOnClickListener {
            prefs.edit().remove("custom_mp3_uris").apply() // "uris" mit s!
            updateUi()
            Toast.makeText(this, "Standard-Ton reaktiviert", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btn_test_audio).setOnClickListener {
            startActivity(Intent(this, AlarmScreenActivity::class.java))
        }

        updateUi()
    }

    private fun updateUi() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        // Wir arbeiten nur noch mit dem Set "custom_mp3_uris"
        val uriSet = prefs.getStringSet("custom_mp3_uris", null)
        val tvFileName = findViewById<TextView>(R.id.tv_current_filename)

        if (uriSet.isNullOrEmpty()) {
            // Fall 1: Liste ist leer oder gelöscht -> Standard
            tvFileName.text = "Aktuell: Standard-Ton"
        } else if (uriSet.size == 1) {
            // Fall 2: Genau eine Datei ausgewählt -> Namen dieser einen Datei anzeigen
            val singleUri = Uri.parse(uriSet.first())
            tvFileName.text = "Datei: ${getFileName(singleUri)}"
        } else {
            // Fall 3: Mehrere Dateien -> Anzahl anzeigen
            tvFileName.text = "Aktiv: ${uriSet.size} Töne (Zufall)"
        }
    }
    private fun getFileName(uri: Uri): String {
        var name = "Eigener Ton"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) name = cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            name = uri.path?.substringAfterLast('/') ?: "Datei"
        }
        return name
    }
}