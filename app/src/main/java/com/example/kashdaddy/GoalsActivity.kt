package com.example.kashdaddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GoalsActivity : AppCompatActivity() {
    private lateinit var goalRecyclerView: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<Goal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        goalRecyclerView = findViewById(R.id.goal_recycler_view)
        goalRecyclerView.layoutManager = LinearLayoutManager(this)

        // Add some dummy goals (in a real app, fetch from Firebase or local storage)
        goalList.add(Goal("Save for Vacation", 3000, 1500))
        goalList.add(Goal("Pay off Debt", 5000, 2500))
        goalList.add(Goal("Emergency Fund", 10000, 2000))

        goalAdapter = GoalAdapter(goalList)
        goalRecyclerView.adapter = goalAdapter
    }
}

data class Goal(val name: String, val targetAmount: Int, val currentAmount: Int)


