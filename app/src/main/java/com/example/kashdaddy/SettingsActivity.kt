package com.example.kashdaddy

import android.content.Intent
import android.view.View
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView
    private lateinit var budgetCategoryEditText: EditText
    private lateinit var expenseLimitEditText: EditText
    private lateinit var notificationSwitch: Switch
    private lateinit var languageSpinner: Spinner
    private lateinit var changePasswordEditText: EditText
    private lateinit var appLockSwitch: Switch
    private lateinit var updateSettingsButton: Button
    private lateinit var logoutButton: Button
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val userRef: DatabaseReference by lazy {
        database.reference.child("users").child(firebaseAuth.currentUser?.uid ?: "")
    }

    private var selectedLanguage: String = "English" // Default language

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        profileNameTextView = findViewById(R.id.profileName)
        profileEmailTextView = findViewById(R.id.profileEmail)
        budgetCategoryEditText = findViewById(R.id.budgetCategoryEditText)
        expenseLimitEditText = findViewById(R.id.expenseLimitEditText)
        notificationSwitch = findViewById(R.id.NotificationSwitch)
        languageSpinner = findViewById(R.id.languageSpinner)
        changePasswordEditText = findViewById(R.id.changePasswordEditText)
        appLockSwitch = findViewById(R.id.appLockSwitch)
        updateSettingsButton = findViewById(R.id.updateSettingsButton)
        logoutButton = findViewById(R.id.LogoutButton)

        // Set up language spinner
        val languages = arrayOf("English", "Zulu", "Afrikaans")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        languageSpinner.adapter = adapter

        // Handle language selection change and store the selected language
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedLanguage = languages[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Load user data
        loadUserData()

        // Handle update settings button click
        updateSettingsButton.setOnClickListener {
            updateChanges()
        }

        // Handle logout button click
        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun changeLanguage(language: String) {
        val locale = when (language) {
            "Zulu" -> Locale("zu")
            "Afrikaans" -> Locale("af")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val resources: Resources = resources
        val config: Configuration = resources.configuration
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        config.setLocale(locale)
        resources.updateConfiguration(config, displayMetrics)
        recreate() // Recreate the activity to apply changes
    }

    private fun loadUserData() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            profileEmailTextView.text = user.email

            userRef.child("name").get().addOnSuccessListener {
                val name = it.value as? String ?: "Unknown User"
                profileNameTextView.text = name
            }
            userRef.child("budgetCategory").get().addOnSuccessListener {
                val budgetCategory = it.value as? String ?: ""
                budgetCategoryEditText.setText(budgetCategory)
            }
            userRef.child("expenseLimit").get().addOnSuccessListener {
                val expenseLimit = it.value as? Double
                expenseLimitEditText.setText(expenseLimit?.toString() ?: "")
            }
            userRef.child("enableReminders").get().addOnSuccessListener {
                val enableReminders = it.value as? Boolean ?: false
                notificationSwitch.isChecked = enableReminders
            }
            userRef.child("appLock").get().addOnSuccessListener {
                val appLock = it.value as? Boolean ?: false
                appLockSwitch.isChecked = appLock
            }
        }
    }

    private fun updateChanges() {
        val newBudgetCategory = budgetCategoryEditText.text.toString()
        val newExpenseLimit = expenseLimitEditText.text.toString().toDoubleOrNull() ?: 0.0
        val newEnableReminders = notificationSwitch.isChecked
        val newAppLock = appLockSwitch.isChecked
        val updates = mutableMapOf<String, Any>()

        // Collect updates if they have changed
        updates["budgetCategory"] = newBudgetCategory
        updates["expenseLimit"] = newExpenseLimit
        updates["enableReminders"] = newEnableReminders
        updates["appLock"] = newAppLock

        // Apply language change
        changeLanguage(selectedLanguage)

        userRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Data successfully updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
