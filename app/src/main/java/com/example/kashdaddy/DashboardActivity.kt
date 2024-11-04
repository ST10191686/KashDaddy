package com.example.kashdaddy

import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

// CardView Implementation
// Android Developer Documentation
// Title: CardView
// Date: October 2023
// Source: Android Studio Documentation
class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Find the CardViews
        val statisticsCard = findViewById<CardView>(R.id.statistics_card)
        val goalsCard = findViewById<CardView>(R.id.goals_card)
        val transactionsCard = findViewById<CardView>(R.id.transactions_card)
        val resourcesCard = findViewById<CardView>(R.id.resources_card)
        val remindersCard = findViewById<CardView>(R.id.reminders_card)
        val settingsCard = findViewById<CardView>(R.id.settings_card)

        // Set click listeners
        statisticsCard.setOnClickListener {
            // Handle Statistics click
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        goalsCard.setOnClickListener {
            // Handle Goals click
            startActivity(Intent(this, GoalsActivity::class.java))
        }

        transactionsCard.setOnClickListener {
            // Handle Transactions click
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        resourcesCard.setOnClickListener {
            // Handle Resources click
            startActivity(Intent(this, ResourcesActivity::class.java))
        }

        remindersCard.setOnClickListener {
            // Handle Reminders click
            startActivity(Intent(this, RemindersActivity::class.java))
        }

        settingsCard.setOnClickListener {
            // Handle Settings click
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
