package com.example.kashdaddy

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var profileNameEditText: EditText
    private lateinit var profileEmailEditText: EditText
    private lateinit var budgetCategoryEditText: EditText
    private lateinit var expenseLimitEditText: EditText
    private lateinit var enableRemindersSwitch: Switch
    private lateinit var expenseAlertsSwitch: Switch
    private lateinit var changePasswordEditText: EditText
    private lateinit var appLockSwitch: Switch
    private lateinit var updateSettingsButton: Button

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val userRef: DatabaseReference by lazy { database.reference.child("users").child(firebaseAuth.uid ?: "") }

    private var previousName: String = ""
    private var previousEmail: String = ""
    private var previousBudgetCategory: String = ""
    private var previousExpenseLimit: Double? = null
    private var previousEnableReminders: Boolean = false
    private var previousExpenseAlerts: Boolean = false
    private var previousAppLock: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        profileNameEditText = findViewById(R.id.profileNameEditText)
        profileEmailEditText = findViewById(R.id.profileEmailEditText)
        budgetCategoryEditText = findViewById(R.id.budgetCategoryEditText)
        expenseLimitEditText = findViewById(R.id.expenseLimitEditText)
        enableRemindersSwitch = findViewById(R.id.enableRemindersSwitch)
        expenseAlertsSwitch = findViewById(R.id.expenseAlertsSwitch)
        changePasswordEditText = findViewById(R.id.changePasswordEditText)
        appLockSwitch = findViewById(R.id.appLockSwitch)
        updateSettingsButton = findViewById(R.id.updateSettingsButton)

        // Load user data
        loadUserData()

        // Handle button click
        updateSettingsButton.setOnClickListener {
            updateChanges()
        }
    }

    private fun loadUserData() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            profileEmailEditText.setText(user.email)
            userRef.child("name").get().addOnSuccessListener {
                previousName = it.value as? String ?: ""
                profileNameEditText.setText(previousName)
            }
            userRef.child("budgetCategory").get().addOnSuccessListener {
                previousBudgetCategory = it.value as? String ?: ""
                budgetCategoryEditText.setText(previousBudgetCategory)
            }
            userRef.child("expenseLimit").get().addOnSuccessListener {
                previousExpenseLimit = it.value as? Double
                expenseLimitEditText.setText(previousExpenseLimit?.toString() ?: "")
            }
            userRef.child("enableReminders").get().addOnSuccessListener {
                previousEnableReminders = it.value as? Boolean ?: false
                enableRemindersSwitch.isChecked = previousEnableReminders
            }
            userRef.child("expenseAlerts").get().addOnSuccessListener {
                previousExpenseAlerts = it.value as? Boolean ?: false
                expenseAlertsSwitch.isChecked = previousExpenseAlerts
            }
            userRef.child("appLock").get().addOnSuccessListener {
                previousAppLock = it.value as? Boolean ?: false
                appLockSwitch.isChecked = previousAppLock
            }
        }
    }

    private fun updateChanges() {
        val newName = profileNameEditText.text.toString()
        val newBudgetCategory = budgetCategoryEditText.text.toString()
        val newExpenseLimit = expenseLimitEditText.text.toString().toDoubleOrNull()
        val newEnableReminders = enableRemindersSwitch.isChecked
        val newExpenseAlerts = expenseAlertsSwitch.isChecked
        val newAppLock = appLockSwitch.isChecked

        val updates = mutableMapOf<String, Any>()

        if (newName != previousName) {
            updates["name"] = newName
        }
        if (newBudgetCategory != previousBudgetCategory) {
            updates["budgetCategory"] = newBudgetCategory
        }
        if (newExpenseLimit != previousExpenseLimit) {
            updates["expenseLimit"] = newExpenseLimit ?: 0.0
        }
        if (newEnableReminders != previousEnableReminders) {
            updates["enableReminders"] = newEnableReminders
        }
        if (newExpenseAlerts != previousExpenseAlerts) {
            updates["expenseAlerts"] = newExpenseAlerts
        }
        if (newAppLock != previousAppLock) {
            updates["appLock"] = newAppLock
        }

        if (updates.isNotEmpty()) {
            userRef.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(this, "Data successfully updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
        }
    }
}
