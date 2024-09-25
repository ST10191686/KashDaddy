package com.example.kashdaddy

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.* // Import Firebase database
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

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

        // Show layout for adding a new goal when the "Add Goal" button is clicked
        addGoalButton.setOnClickListener {
            addGoalLayout.visibility = View.VISIBLE
        }

        // Save the new goal to Firebase and hide the form when "Save Goal" button is clicked
        saveGoalButton.setOnClickListener {
            val goalName = goalNameEditText.text.toString()
            val goalCategory = addGoalLayout.findViewById<EditText>(R.id.goal_category).text.toString()
            val goalTargetAmount = goalTargetAmountEditText.text.toString().toIntOrNull()
            val goalDueDate = addGoalLayout.findViewById<EditText>(R.id.goal_due_date).text.toString()

            if (goalName.isNotEmpty() && goalTargetAmount != null && goalCategory.isNotEmpty() && goalDueDate.isNotEmpty()) {
                val goalId = database.push().key // Generate a unique ID for each goal
                if (goalId != null) {
                    val timeRemaining = calculateTimeRemaining(goalDueDate)
                    val newGoal = Goal(goalName, goalCategory, goalTargetAmount, 0, goalDueDate, timeRemaining)
                    database.child(goalId).setValue(newGoal) // Save goal to Firebase

                    // Add the new goal to the local list and notify the adapter
                    goalList.add(newGoal)
                    goalAdapter.notifyDataSetChanged()

                    // Hide the add goal layout and clear inputs
                    addGoalLayout.visibility = View.GONE
                    goalNameEditText.text.clear()
                    goalTargetAmountEditText.text.clear()
                    addGoalLayout.findViewById<EditText>(R.id.goal_category).text.clear()
                    addGoalLayout.findViewById<EditText>(R.id.goal_due_date).text.clear()
                }
            }
        }

        // Load goals from Firebase
        loadGoalsFromFirebase()
    }

    private fun calculateTimeRemaining(dueDate: String): String {
        // Define the date format that matches the input due date (e.g., "dd/MM/yyyy")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try {
            // Parse the due date
            val dueDateParsed = dateFormat.parse(dueDate)

            // Get the current date
            val currentDate = Calendar.getInstance().time

            // Calculate the difference in milliseconds
            val diffInMillis = dueDateParsed.time - currentDate.time

            // Convert the difference to days
            val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            // Return the appropriate message based on the remaining time
            when {
                daysRemaining > 0 -> "$daysRemaining days remaining"
                daysRemaining == 0 -> "Due today!"
                else -> "Past due"
            }
        } catch (e: Exception) {
            // Handle any parsing errors
            "Invalid due date"
        }
    }

    // Function to load goals from Firebase
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
