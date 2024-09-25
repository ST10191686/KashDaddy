package com.example.kashdaddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(private val goals: MutableList<Goal>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val goalName: TextView = view.findViewById(R.id.goal_name)
        val goalProgressBar: ProgressBar = view.findViewById(R.id.goal_progress_bar)
        val goalProgressText: TextView = view.findViewById(R.id.goal_progress_text)
        val goalCategory: TextView = view.findViewById(R.id.goal_category) // Add category view
        val goalDueDate: TextView = view.findViewById(R.id.goal_due_date) // Add due date view
        val goalTimeRemaining: TextView = view.findViewById(R.id.goal_time_remaining) // Add time remaining view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        holder.goalName.text = goal.name
        holder.goalProgressText.text = "${goal.currentAmount} / ${goal.targetAmount}"
        holder.goalProgressBar.progress = if (goal.targetAmount > 0) (goal.currentAmount * 100) / goal.targetAmount else 0
        holder.goalCategory.text = goal.category
        holder.goalDueDate.text = "Achieve by: ${goal.dueDate}"
        holder.goalTimeRemaining.text = goal.timeRemaining
    }

    override fun getItemCount() = goals.size
}

