package com.example.kashdaddy

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var incomeReference: DatabaseReference
    private lateinit var pieChart: PieChart
    private lateinit var textTotalBalance: TextView
    private lateinit var textIncome: TextView
    private lateinit var textExpenses: TextView
    private lateinit var textMonthName: TextView

    private var userIncome: Double = 0.0 // Default income value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        pieChart = findViewById(R.id.pieChart)
        textTotalBalance = findViewById(R.id.textTotalBalance)
        textIncome = findViewById(R.id.textIncome)
        textExpenses = findViewById(R.id.textExpenses)
        textMonthName = findViewById(R.id.textMonthName)

        // Set up buttons
        findViewById<Button>(R.id.buttonCurrent).setOnClickListener { updateStats("current") }
        findViewById<Button>(R.id.button3m).setOnClickListener { updateStats("3m") }
        findViewById<Button>(R.id.button6m).setOnClickListener { updateStats("6m") }
        findViewById<Button>(R.id.button1y).setOnClickListener { updateStats("1y") }

        // Initialize Firebase references
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/transactions")
        incomeReference = FirebaseDatabase.getInstance().getReference("users/$userId/income")

        // Load user income from settings
        loadUserIncome()
    }

    private fun loadUserIncome() {
        incomeReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userIncome = dataSnapshot.getValue(Double::class.java) ?: 0.0
                Log.d("StatisticsActivity", "User Income Loaded: $userIncome")
                updateStats("current") // Re-fetch statistics to include the updated income value
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("StatisticsActivity", "Failed to load user income", databaseError.toException())
            }
        })
    }

    private fun updateStats(period: String) {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var totalIncome = 0.0
                var totalExpenses = 0.0

                // Calculate total expenses
                for (snapshot in dataSnapshot.children) {
                    val transaction = snapshot.getValue(TransactionsActivity.Transaction::class.java)
                    transaction?.let {
                        if (it.type == "Received") {
                            totalIncome += it.amount
                        } else if (it.type == "Sent") {
                            totalExpenses += it.amount
                        }
                    }
                }

                val totalBalance = userIncome + totalIncome - totalExpenses

                // Update TextViews
                textIncome.text = "Income: R${String.format("%.2f", totalIncome)}"
                textExpenses.text = "Expenses: R${String.format("%.2f", totalExpenses)}"
                textTotalBalance.text = "Total Balance: R${String.format("%.2f", totalBalance)}"
                textMonthName.text = "Month: ${getMonthName(period)}"

                // Update Pie Chart
                updatePieChart(totalIncome, totalExpenses)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("StatisticsActivity", "Failed to load transactions", databaseError.toException())
            }
        })
    }

    private fun updatePieChart(income: Double, expenses: Double) {
        val entries = listOf(
            PieEntry(income.toFloat(), "Income"),
            PieEntry(expenses.toFloat(), "Expenses")
        )

        val dataSet = PieDataSet(entries, "Budget Breakdown")
        dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#FFC107")) // Green and Yellow
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.WHITE)

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = false
            legend.isEnabled = true
            legend.textSize = 14f
            legend.formSize = 14f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1400, Easing.EaseInOutQuad)
            invalidate() // Refresh chart
        }
    }

    private fun getMonthName(period: String): String {
        return when (period) {
            "current" -> "Current"
            "3m" -> "Last 3 Months"
            "6m" -> "Last 6 Months"
            "1y" -> "Last Year"
            else -> "Unknown"
        }
    }
}
