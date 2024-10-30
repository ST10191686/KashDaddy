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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.NotificationCompat

class GoalsActivity : AppCompatActivity() {

    private lateinit var goalRecyclerView: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<Goal>()
    private lateinit var addGoalButton: Button
    private lateinit var saveGoalButton: Button
    private lateinit var goalNameEditText: EditText
    private lateinit var goalTargetAmountEditText: EditText
    private lateinit var addGoalLayout: LinearLayout

    // Firebase reference
    private lateinit var database: DatabaseReference
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // SharedPreferences for offline goals
    private lateinit var sharedPreferences: SharedPreferences

    // Notification channel ID
    private val CHANNEL_ID = "goal_notifications"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)

        // Initialize Firebase Realtime Database under the user's specific path
        database = Firebase.database.reference.child("users").child(firebaseAuth.currentUser?.uid ?: "").child("goals")

        // Initialize RecyclerView
        goalRecyclerView = findViewById(R.id.goal_recycler_view)
        goalRecyclerView.layoutManager = LinearLayoutManager(this)

        // Adapter setup
        goalAdapter = GoalAdapter(goalList)
        goalRecyclerView.adapter = goalAdapter

        // Button and input views
        addGoalButton = findViewById(R.id.add_goal_button)
        saveGoalButton = findViewById(R.id.save_goal_button)
        goalNameEditText = findViewById(R.id.goal_name)
        goalTargetAmountEditText = findViewById(R.id.goal_target_amount)
        addGoalLayout = findViewById(R.id.add_goal_layout)

        // Create notification channel
        createNotificationChannel()

        // Show layout for adding a new goal when the "Add Goal" button is clicked
        addGoalButton.setOnClickListener {
            addGoalLayout.visibility = View.VISIBLE
        }

        // Save the new goal
        saveGoalButton.setOnClickListener {
            val goalName = goalNameEditText.text.toString()
            val goalCategory = addGoalLayout.findViewById<EditText>(R.id.goal_category).text.toString()
            val goalTargetAmount = goalTargetAmountEditText.text.toString().toIntOrNull()
            val goalDueDate = addGoalLayout.findViewById<EditText>(R.id.goal_due_date).text.toString()

            if (goalName.isNotEmpty() && goalTargetAmount != null && goalCategory.isNotEmpty() && goalDueDate.isNotEmpty()) {
                val timeRemaining = calculateTimeRemaining(goalDueDate)
                val newGoal = Goal(goalName, goalCategory, goalTargetAmount, 0, goalDueDate, timeRemaining)

                if (isNetworkAvailable()) {
                    // Save to Firebase
                    saveGoalToFirebase(newGoal)
                } else {
                    // Save offline in SharedPreferences
                    saveGoalOffline(newGoal)
                    Toast.makeText(this, "No internet connection. Goal will be synced later.", Toast.LENGTH_SHORT).show()
                }

                // Hide the add goal layout and clear inputs
                addGoalLayout.visibility = View.GONE
                goalNameEditText.text.clear()
                goalTargetAmountEditText.text.clear()
                addGoalLayout.findViewById<EditText>(R.id.goal_category).text.clear()
                addGoalLayout.findViewById<EditText>(R.id.goal_due_date).text.clear()
            }
        }

        // Load goals from Firebase
        loadGoalsFromFirebase()

        // Sync any offline goals when network is available
        syncOfflineGoals()
    }

    private fun saveGoalToFirebase(goal: Goal) {
        val goalId = database.push().key // Generate a unique ID for each goal
        if (goalId != null) {
            database.child(goalId).setValue(goal)
                .addOnSuccessListener {
                    Toast.makeText(this, "Goal saved!", Toast.LENGTH_SHORT).show()
                    goalList.add(goal)
                    goalAdapter.notifyDataSetChanged()

                    // Show notification for the goal's due date
                    showGoalNotification(goal)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showGoalNotification(goal: Goal) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, GoalsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Add the FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setContentTitle("Goal Reminder")
            .setContentText("${goal.name}: ${goal.timeRemaining}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(goal.hashCode(), notificationBuilder.build())
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Goal Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for goal notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveGoalOffline(goal: Goal) {
        val offlineGoals = sharedPreferences.getStringSet("offlineGoals", mutableSetOf())?.toMutableSet()
        offlineGoals?.add(Gson().toJson(goal))
        sharedPreferences.edit().putStringSet("offlineGoals", offlineGoals).apply()
    }

    private fun syncOfflineGoals() {
        if (isNetworkAvailable()) {
            val offlineGoals = sharedPreferences.getStringSet("offlineGoals", mutableSetOf())
            if (!offlineGoals.isNullOrEmpty()) {
                for (goalJson in offlineGoals) {
                    val goal = Gson().fromJson(goalJson, Goal::class.java)
                    saveGoalToFirebase(goal)
                }
                // Clear offline goals once synced
                sharedPreferences.edit().remove("offlineGoals").apply()
            }
        }
    }

    private fun loadGoalsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                goalList.clear() // Clear the list before loading new data
                for (goalSnapshot in snapshot.children) {
                    val goal = goalSnapshot.getValue(Goal::class.java)
                    if (goal != null) {
                        goalList.add(goal)
                    }
                }
                goalAdapter.notifyDataSetChanged() // Update RecyclerView with the loaded data
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors
            }
        })
    }

    private fun calculateTimeRemaining(dueDate: String): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try {
            val dueDateParsed = dateFormat.parse(dueDate)
            val currentDate = Calendar.getInstance().time
            val diffInMillis = dueDateParsed.time - currentDate.time
            val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            when {
                daysRemaining > 0 -> "$daysRemaining days remaining"
                daysRemaining == 0 -> "Due today!"
                else -> "Past due"
            }
        } catch (e: Exception) {
            "Invalid due date"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}

// Data class for goals
data class Goal(
    val name: String = "",
    val category: String = "",
    val targetAmount: Int = 0,
    val currentAmount: Int = 0,
    val dueDate: String = "",
    val timeRemaining: String = ""
)
