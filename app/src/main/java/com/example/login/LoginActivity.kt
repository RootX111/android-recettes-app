package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etClientName = findViewById<EditText>(R.id.etClientName)
        val cbRemember = findViewById<CheckBox>(R.id.cbRemember)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        prefs = getSharedPreferences("RecettesPrefs", MODE_PRIVATE)

        etClientName.setText(prefs.getString("CLIENT", ""))
        cbRemember.isChecked = prefs.getBoolean("REMEMBER", false)

        btnLogin.setOnClickListener {
            val editor = prefs.edit()

            if (cbRemember.isChecked) {
                editor.putString("CLIENT", etClientName.text.toString())
                editor.putBoolean("REMEMBER", true)
            } else {
                editor.clear()
            }

            editor.apply()

            val intent = Intent(this, ListeRecettesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}