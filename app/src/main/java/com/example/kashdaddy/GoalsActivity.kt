package com.example.kashdaddy

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.* // Import Firebase database
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        // Initialize Firebase Realtime Database
        database = Firebase.database.reference.child("goals")

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
            val goalTargetAmount = goalTargetAmountEditText.text.toString().toIntOrNull()

            if (goalName.isNotEmpty() && goalTargetAmount != null) {
                val goalId = database.push().key // Generate a unique ID for each goal
                if (goalId != null) {
                    val newGoal = Goal(goalName, goalTargetAmount, 0)
                    database.child(goalId).setValue(newGoal) // Save goal to Firebase

                    // Add the new goal to the local list and notify the adapter
                    goalList.add(newGoal)
                    goalAdapter.notifyDataSetChanged()

                    // Hide the add goal layout and clear inputs
                    addGoalLayout.visibility = View.GONE
                    goalNameEditText.text.clear()
                    goalTargetAmountEditText.text.clear()
                }
            }
        }

        // Load goals from Firebase
        loadGoalsFromFirebase()
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
data class Goal(val name: String = "", val targetAmount: Int = 0, val currentAmount: Int = 0)
