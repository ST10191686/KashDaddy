package com.example.kashdaddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class ResourcesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resources)

        // Reference to the article and video containers
        val expenseManagement = findViewById<LinearLayout>(R.id.expense_management)
        val budgetBasics = findViewById<LinearLayout>(R.id.budget_basics)
        val personalBudget = findViewById<LinearLayout>(R.id.personal_budget)
        val maximizeSavings = findViewById<LinearLayout>(R.id.maximize_savings)
        val budgetingBasicsVideo = findViewById<LinearLayout>(R.id.budgeting_basics_video)

        // Set click listeners to open external URLs in the browser
        expenseManagement.setOnClickListener {
            openWebPage("https://www.example.com/expense-management")
        }

        budgetBasics.setOnClickListener {
            openWebPage("https://www.example.com/budget-basics")
        }

        personalBudget.setOnClickListener {
            openWebPage("https://www.example.com/personal-budget")
        }

        maximizeSavings.setOnClickListener {
            openWebPage("https://www.example.com/maximize-savings")
        }

        budgetingBasicsVideo.setOnClickListener {
            openWebPage("https://www.example.com/budgeting-basics-video")
        }
    }

    // Function to open a webpage in the browser
    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }
}
