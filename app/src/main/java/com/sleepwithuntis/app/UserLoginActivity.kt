package com.sleepwithuntis.app

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import de.keule.webuntis.response.School
import de.keule.webuntis.WebUntis
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.EditText
import android.graphics.Color
import android.content.Intent
import android.widget.Button
import android.widget.Toast
import webuntisjava.UntisClient

class UserLoginActivity : AppCompatActivity() {

    private var selectedServerUrl: String = ""
    private var selectedSchoolId: String = ""

    private lateinit var schoolSearchField: MaterialAutoCompleteTextView
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveButton: Button // Neu

    private var searchJob: Job? = null
    private var lastResults: List<School> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#FFF3E0")
        window.navigationBarColor = Color.parseColor("#FFF3E0")
        setContentView(R.layout.activity_user_login)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
        toolbar.navigationIcon?.setTint(Color.parseColor("#BF360C"))

        schoolSearchField = findViewById(R.id.edit_text_school)
        usernameEditText = findViewById(R.id.edit_text_username)
        passwordEditText = findViewById(R.id.edit_text_password)
        saveButton = findViewById(R.id.btn_save_login) // Neu

        loadUserData()
        setupAutoComplete()

        // Der Button speichert jetzt explizit
        saveButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || selectedServerUrl.isEmpty()) {
                Toast.makeText(this, "Bitte fülle alle Felder aus!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val untis = WebUntis(username, password, selectedSchoolId, selectedServerUrl)

                    val loginErfolgreich = untis.login()

                    withContext(Dispatchers.Main) {
                        if (loginErfolgreich) {
                            saveUserData()
                            Toast.makeText(this@UserLoginActivity, "Anmeldung erfolgreich!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {

                            Toast.makeText(this@UserLoginActivity, "Login fehlgeschlagen: Daten prüfen!", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = e.message ?: ""
                        Log.e("UNTIS_LOGIN", "Fehler: $errorMessage")

                        // Wir prüfen, was im Fehlertext steht
                        if (errorMessage.contains("bad credentials", ignoreCase = true) ||
                            errorMessage.contains("invalid", ignoreCase = true)) {
                            Toast.makeText(this@UserLoginActivity, "Benutzerdaten falsch!", Toast.LENGTH_LONG).show()
                        } else {
                            // Das ist der echte Netzwerkfehler
                            Toast.makeText(this@UserLoginActivity, "Netzwerkfehler. Später erneut versuchen.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupAutoComplete() {
        schoolSearchField.setOnClickListener { schoolSearchField.setText("") }

        schoolSearchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 2) performSearch(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Ersetze deinen vorhandenen setOnItemClickListener in setupAutoComplete() durch diesen:

        schoolSearchField.setOnItemClickListener { parent, _, position, _ ->
            val school = lastResults[position]
            val rawUrl = school.serverUrl
            selectedServerUrl = if (rawUrl.contains("/WebUntis/")) {
                rawUrl.substringBefore("/WebUntis/")
            } else {
                rawUrl
            }
            selectedSchoolId = school.loginName
            schoolSearchField.setText(school.displayName, false)
            schoolSearchField.dismissDropDown()
            schoolSearchField.clearFocus()
            hideKeyboard()

            Log.d("SELECTION", "Schule gewählt: ${school.displayName} ID: $selectedSchoolId")
        }
    }


    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        schoolSearchField.setText(sharedPref.getString("school_display_name", ""), false)
        usernameEditText.setText(sharedPref.getString("username", ""))
        passwordEditText.setText(sharedPref.getString("password", ""))

        // Auch die versteckten Werte laden, damit sie beim Speichern nicht leer sind,
        // falls die Schule nicht neu ausgewählt wurde!
        selectedServerUrl = sharedPref.getString("server", "") ?: ""
        selectedSchoolId = sharedPref.getString("school", "") ?: ""
    }

    private fun saveUserData() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", usernameEditText.text.toString())
            putString("password", passwordEditText.text.toString())
            putString("school_display_name", schoolSearchField.text.toString())
            putString("server", selectedServerUrl)
            putString("school", selectedSchoolId)
            apply()
        }
        Log.d("SAVE_CHECK", "Erfolgreich gespeichert")
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(300)
            try {
                val result = WebUntis.searchSchool(query)
                val schools = result.getSchools() ?: emptyList()
                withContext(Dispatchers.Main) {
                    val displayStrings = schools.map { "${it.displayName}\n${it.address}" }
                    lastResults = schools
                    val adapter = ArrayAdapter(this@UserLoginActivity, android.R.layout.simple_list_item_1, displayStrings)
                    schoolSearchField.setAdapter(adapter)
                    if (schoolSearchField.hasFocus()) schoolSearchField.showDropDown()
                }
            } catch (e: Exception) { Log.e("UNTIS_ERROR", "Suche fehlgeschlagen") }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm?.hideSoftInputFromWindow(schoolSearchField.windowToken, 0)
    }
}