package com.example.kashdaddy

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("reminders")
    private val reminderTexts = mutableSetOf<String>() // Using a Set to avoid duplicates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

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
            reminderForm.visibility = if (reminderForm.visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Save reminder
        saveReminderButton.setOnClickListener {
            val title = reminderTitleEditText.text.toString()
            val amount = reminderAmountEditText.text.toString()

            if (title.isNotEmpty() && amount.isNotEmpty()) {
                val date = selectedDateTextView.text.toString().removePrefix("Selected Date: ")
                val reminderText = "$title - Amount: $amount - Date: $date"

                // Save reminder to Firebase
                val reminderId = database.push().key
                if (reminderId != null) {
                    database.child(reminderId).setValue(reminderText)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()
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
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Load reminders from Firebase
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
                Toast.makeText(this@RemindersActivity, "Failed to load reminders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addReminderToView(reminderText: String) {
        val textView = TextView(this)
        textView.text = reminderText
        textView.setPadding(8, 8, 8, 8)
        textView.textSize = 16f
        remindersContainer.addView(textView)
    }
}
