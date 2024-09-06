package com.example.kashdaddy
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(private val goals: List<Goal>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val goalName: TextView = view.findViewById(R.id.goal_name)
        val goalProgressBar: ProgressBar = view.findViewById(R.id.goal_progress_bar)
        val goalProgressText: TextView = view.findViewById(R.id.goal_progress_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.goalName.text = goal.name
        val progress = (goal.currentAmount * 100) / goal.targetAmount
        holder.goalProgressBar.progress = progress
        holder.goalProgressText.text = "${goal.currentAmount} / ${goal.targetAmount}"
    }

    override fun getItemCount() = goals.size
}
