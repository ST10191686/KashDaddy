package com.example.kashdaddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View

class ResourcesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resources) // Ensure you are referencing the correct layout
    }

    // Create Clickable Hyperlinks in TextView in Android with Kotlin
    // https://www.geeksforgeeks.org/create-clickable-hyperlinks-in-textview-in-android-with-kotlin/
    // aashaypawar
    // 

    // Function to handle article clicks
    fun onArticleClick(view: View) {
        val url = when (view.id) {
            R.id.expense_management -> "https://www.rydoo.com/cfo-corner/what-is-expense-management/"
            R.id.budget_basics -> "https://bettermoneyhabits.bankofamerica.com/en/saving-budgeting/creating-a-budget"
            R.id.personal_budget -> "https://dfr.oregon.gov/financial/manage/pages/budget.aspx"
            R.id.maximize_savings -> "https://fastercapital.com/content/Top-10-Tips-for-Maximizing-Your-Savings-Account.html"
            else -> ""
        }

        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    // Function to handle video clicks
    fun onVideoClick(view: View) {
        val videoUrl = when (view.id) {
            R.id.budgeting_basics_video -> "https://www.youtube.com/watch?v=sVKQn2I4HDM"
            else -> ""
        }

        if (videoUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(videoUrl)
            startActivity(intent)
        }
    }
}

