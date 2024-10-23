package com.example.kashdaddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson

class TransactionsActivity : AppCompatActivity() {

    private val transactionList = mutableListOf<Transaction>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var currentFilter: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        // Initialize SharedPreferences for offline storage
        sharedPreferences = getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.transactions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TransactionAdapter(transactionList)

        // Handle the Add Transaction button click
        val addTransactionButton = findViewById<Button>(R.id.add_transaction_button)
        addTransactionButton.setOnClickListener {
            showAddTransactionLayout()
        }

        // Initialize filter buttons
        val allButton = findViewById<Button>(R.id.filterAll)
        val sentButton = findViewById<Button>(R.id.filterSpent)
        val receivedButton = findViewById<Button>(R.id.filterReceived)
        val dateButton = findViewById<Button>(R.id.filterDate)
        val categoryButton = findViewById<Button>(R.id.filterCategory)

        // Set button click listeners for filtering
        allButton.setOnClickListener { applyFilter("All") }
        sentButton.setOnClickListener { applyFilter("Sent") }
        receivedButton.setOnClickListener { applyFilter("Received") }
        dateButton.setOnClickListener { applyFilter("Date") }
        categoryButton.setOnClickListener { applyFilter("Category") }

        // Load transactions from Firebase database
        loadTransactions()

        // Sync any offline data once the network is available
        syncOfflineTransactions()

        currentFilter = "All" // Set default filter
    }

    private fun applyFilter(filterType: String) {
        currentFilter = filterType
        val filteredList = when (filterType) {
            "Sent" -> transactionList.filter { it.type == "Sent" }
            "Received" -> transactionList.filter { it.type == "Received" }
            "Date" -> transactionList.sortedBy { it.date } // Sorting by date
            "Category" -> transactionList.sortedBy { it.category } // Sorting by category
            else -> transactionList // "All" returns the full list
        }
        recyclerView.adapter = TransactionAdapter(filteredList)
    }

    private fun showAddTransactionLayout() {
        setContentView(R.layout.activity_add_transactions)

        val sentReceivedSpinner = findViewById<Spinner>(R.id.sentReceivedSpinner)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)

        // Sample data for the spinners
        val sentReceivedOptions = arrayOf("Sent", "Received")
        val categoryOptions = arrayOf("Food", "Transport", "Entertainment", "Other")

        val sentReceivedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sentReceivedOptions)
        sentReceivedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sentReceivedSpinner.adapter = sentReceivedAdapter

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val addTransactionButton = findViewById<Button>(R.id.addTransactionButton)
        addTransactionButton.setOnClickListener {
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        val title = findViewById<EditText>(R.id.titleEditText).text.toString().trim()
        val amount = findViewById<EditText>(R.id.amountEditText).text.toString().toDoubleOrNull() ?: 0.0
        val date = findViewById<EditText>(R.id.dateEditText).text.toString().trim()
        val type = findViewById<Spinner>(R.id.sentReceivedSpinner).selectedItem.toString()
        val toFrom = findViewById<EditText>(R.id.toFromEditText).text.toString().trim()
        val category = findViewById<Spinner>(R.id.categorySpinner).selectedItem.toString()
        val description = findViewById<EditText>(R.id.descriptionEditText).text.toString().trim()

        if (title.isEmpty() || date.isEmpty() || toFrom.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new Transaction object
        val newTransaction = Transaction(
            title = title,
            amount = amount,
            date = date,
            type = type,
            toFrom = toFrom,
            category = category,
            description = description
        )

        if (isNetworkAvailable()) {
            // Save to Firebase under the user's unique path
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/transactions")
            val transactionId = databaseReference.push().key
            if (transactionId != null) {
                databaseReference.child(transactionId).setValue(newTransaction).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Transaction added successfully.", Toast.LENGTH_SHORT).show()
                        navigateToTransactions() // Navigate back to transactions activity
                    } else {
                        Toast.makeText(this, "Failed to add transaction.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Save the transaction offline in SharedPreferences
            saveTransactionOffline(newTransaction)
            Toast.makeText(this, "No internet connection. Transaction will be synced later.", Toast.LENGTH_SHORT).show()

            // Redirect to transactions activity as if it was online
            navigateToTransactions() // Use a method to navigate to the transactions activity
        }
    }

    private fun navigateToTransactions() {
        val intent = Intent(this, TransactionsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK // Clear previous activities
        startActivity(intent)
    }


    private fun saveTransactionOffline(transaction: Transaction) {
        // Save the transaction offline
        val offlineTransactions = sharedPreferences.getStringSet("offlineTransactions", mutableSetOf())?.toMutableSet()
        offlineTransactions?.add(Gson().toJson(transaction))
        sharedPreferences.edit().putStringSet("offlineTransactions", offlineTransactions).apply()

        // Immediately add the transaction to the transaction list and update RecyclerView
        transactionList.add(transaction)
        recyclerView.adapter?.notifyDataSetChanged()

        Toast.makeText(this, "Transaction saved offline and will sync when connected.", Toast.LENGTH_SHORT).show()
    }

    private fun syncOfflineTransactions() {
        if (isNetworkAvailable()) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/transactions")

            val offlineTransactions = sharedPreferences.getStringSet("offlineTransactions", mutableSetOf())
            if (!offlineTransactions.isNullOrEmpty()) {
                for (transactionJson in offlineTransactions) {
                    val transaction = Gson().fromJson(transactionJson, Transaction::class.java)
                    val transactionId = databaseReference.push().key
                    if (transactionId != null) {
                        databaseReference.child(transactionId).setValue(transaction)
                    }
                }

                // Clear the offline transactions once synced
                sharedPreferences.edit().remove("offlineTransactions").apply()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun loadTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/transactions")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                transactionList.clear()
                for (snapshot in dataSnapshot.children) {
                    val transaction = snapshot.getValue(Transaction::class.java)
                    if (transaction != null) {
                        transactionList.add(transaction)
                    }
                }
                applyFilter(currentFilter) // Reapply the current filter after loading
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@TransactionsActivity, "Failed to load transactions.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    data class Transaction(
        val title: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val type: String = "",
        val toFrom: String = "",
        val category: String = "",
        val description: String = ""
    )

    inner class TransactionAdapter(private val transactions: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

        inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val titleView: TextView = view.findViewById(R.id.transactionTitle)
            private val amountView: TextView = view.findViewById(R.id.transactionAmount)
            private val dateView: TextView = view.findViewById(R.id.transactionDate)

            fun bind(transaction: Transaction) {
                titleView.text = transaction.title
                amountView.text = "R${transaction.amount}" // Updated to use Rand currency
                dateView.text = transaction.date
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
            return TransactionViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val transaction = transactions[position]
            holder.bind(transaction)
        }

        override fun getItemCount() = transactions.size
    }
}


