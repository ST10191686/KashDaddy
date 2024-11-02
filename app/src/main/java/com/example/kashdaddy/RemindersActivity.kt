package com.example.kashdaddy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RemindersActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateTextView: TextView
    private lateinit var addReminderButton: Button
    private lateinit var reminderForm: LinearLayout
    private lateinit var reminderTitleEditText: EditText
    private lateinit var reminderAmountEditText: EditText
    private lateinit var saveReminderButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var remindersContainer: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference.child("users").child(firebaseAuth.currentUser?.uid ?: "").child("reminders")
    }
    private val reminderTexts = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        // Initialize SharedPreferences for offline storage
        sharedPreferences = getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)

        // Initialize views
        calendarView = findViewById(R.id.calendarView)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        addReminderButton = findViewById(R.id.addReminderButton)
        reminderForm = findViewById(R.id.reminderForm)
        reminderTitleEditText = findViewById(R.id.reminderTitleEditText)
        reminderAmountEditText = findViewById(R.id.reminderAmountEditText)
        saveReminderButton = findViewById(R.id.saveReminderButton)
        scrollView = findViewById(R.id.scrollView)
        remindersContainer = findViewById(R.id.remindersContainer)

        // Handle calendar date change
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = "$dayOfMonth/${month + 1}/$year"
            selectedDateTextView.text = "Selected Date: $date"
        }

        // Toggle reminder form visibility
        addReminderButton.setOnClickListener {
            reminderForm.visibility = if (reminderForm.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Save reminder
        saveReminderButton.setOnClickListener {
            val title = reminderTitleEditText.text.toString()
            val amount = reminderAmountEditText.text.toString()

            if (title.isNotEmpty() && amount.isNotEmpty()) {
                val date = selectedDateTextView.text.toString().removePrefix("Selected Date: ")
                val reminderText = "$title - Amount: $amount - Date: $date"

                if (isNetworkAvailable()) {
                    // Save to Firebase
                    saveReminderToFirebase(reminderText)
                } else {
                    // Save offline in SharedPreferences
                    saveReminderOffline(reminderText)
                    Toast.makeText(this, "No internet connection. Reminder will be synced later.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Load reminders from Firebase
        loadReminders()

        // Sync any offline reminders when network is available
        syncOfflineReminders()
    }

    private fun saveReminderToFirebase(reminderText: String) {
        val reminderId = database.push().key
        if (reminderId != null) {
            database.child(reminderId).setValue(reminderText)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()

                    // Show notification immediately after saving the reminder
                    showNotification("Reminder: $reminderText", "Due Today")

                    if (reminderTexts.add(reminderText)) {
                        addReminderToView(reminderText)
                    }

                    // Clear form fields
                    reminderTitleEditText.text.clear()
                    reminderAmountEditText.text.clear()

                    // Hide the reminder form
                    reminderForm.visibility = View.GONE
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save reminder", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, "default_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun saveReminderOffline(reminderText: String) {
        val offlineReminders = sharedPreferences.getStringSet("offlineReminders", mutableSetOf())?.toMutableSet()
        offlineReminders?.add(reminderText)
        sharedPreferences.edit().putStringSet("offlineReminders", offlineReminders).apply()
    }

    private fun syncOfflineReminders() {
        if (isNetworkAvailable()) {
            val offlineReminders = sharedPreferences.getStringSet("offlineReminders", mutableSetOf())
            if (!offlineReminders.isNullOrEmpty()) {
                for (reminderText in offlineReminders) {
                    saveReminderToFirebase(reminderText)
                }
                // Clear offline reminders once synced
                sharedPreferences.edit().remove("offlineReminders").apply()
            }
        }
    }

    private fun loadReminders() {
        database.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                remindersContainer.removeAllViews()
                reminderTexts.clear()
                for (child in snapshot.children) {
                    val reminderText = child.value as? String
                    if (reminderText != null && reminderTexts.add(reminderText)) {
                        addReminderToView(reminderText)
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun addReminderToView(reminderText: String) {
        // Split the reminderText string into title, amount, and date
        val parts = reminderText.split(" - ")
        val title = parts.getOrNull(0) ?: "No Title"
        val amount = parts.getOrNull(1) ?: "No Amount"
        val date = parts.getOrNull(2) ?: "No Date"

        // Create a LinearLayout for each reminder entry
        val reminderLayout = LinearLayout(this)
        reminderLayout.orientation = LinearLayout.VERTICAL
        reminderLayout.setPadding(8, 8, 8, 8)

        // Create a TextView for the title
        val titleTextView = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(0, 0, 0, 4)
        }

        // Create a TextView for the amount
        val amountTextView = TextView(this).apply {
            text = amount
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(0, 0, 0, 4)
        }

        // Create a TextView for the due date
        val dateTextView = TextView(this).apply {
            text = date
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(0, 0, 0, 4)
        }

        // Add the title, amount, and date TextViews to the reminder layout
        reminderLayout.addView(titleTextView)
        reminderLayout.addView(amountTextView)
        reminderLayout.addView(dateTextView)

        // Add a separator for each reminder entry
        val separator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        }
        reminderLayout.addView(separator)

        // Add the reminder layout to the container
        remindersContainer.addView(reminderLayout)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
