package com.example.kashdaddy

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TransactionsActivity : AppCompatActivity() {

    // List to hold transactions; initially empty
    private val transactionList = mutableListOf<Transaction>()

    // Reference to the RecyclerView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initially display the transaction history layout
        setContentView(R.layout.activity_transactions)

        // Initialize RecyclerView to display transactions
        recyclerView = findViewById(R.id.transactions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TransactionAdapter(transactionList)

        // Handle the Add Transaction button click
        val addTransactionButton = findViewById<Button>(R.id.add_transaction_button)
        addTransactionButton.setOnClickListener {
            // Switch to the add transaction layout
            showAddTransactionLayout()
        }

        // Load transactions from database
        loadTransactions()
    }

    // Method to switch to the add transaction layout
    private fun showAddTransactionLayout() {
        setContentView(R.layout.activity_add_transactions)

        // Initialize spinners with data
        val sentReceivedSpinner = findViewById<Spinner>(R.id.sentReceivedSpinner)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)

        // Sample data for the spinners
        val sentReceivedOptions = arrayOf("Sent", "Received")
        val categoryOptions = arrayOf("Food", "Transport", "Entertainment", "Other")

        // Set up ArrayAdapters for the spinners
        val sentReceivedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sentReceivedOptions)
        sentReceivedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sentReceivedSpinner.adapter = sentReceivedAdapter

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Handle the Add Transaction button click
        val addTransactionButton = findViewById<Button>(R.id.addTransactionButton)
        addTransactionButton.setOnClickListener {
            saveTransaction()
        }
    }


    // Method to save a new transaction
    private fun saveTransaction() {
        // Get input data
        val title = findViewById<EditText>(R.id.titleEditText).text.toString().trim()
        val amount = findViewById<EditText>(R.id.amountEditText).text.toString().toDoubleOrNull() ?: 0.0
        val date = findViewById<EditText>(R.id.dateEditText).text.toString().trim()
        val type = findViewById<Spinner>(R.id.sentReceivedSpinner).selectedItem.toString()
        val toFrom = findViewById<EditText>(R.id.toFromEditText).text.toString().trim()
        val category = findViewById<Spinner>(R.id.categorySpinner).selectedItem.toString()
        val description = findViewById<EditText>(R.id.descriptionEditText).text.toString().trim()

        if (title.isEmpty() || date.isEmpty() || toFrom.isEmpty() || category.isEmpty()) {
            // Show error message
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

        // Save to Firebase
        val databaseReference = FirebaseDatabase.getInstance().getReference("transactions")
        val transactionId = databaseReference.push().key
        if (transactionId != null) {
            databaseReference.child(transactionId).setValue(newTransaction).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Transaction added successfully.", Toast.LENGTH_SHORT).show()
                    // Switch back to transaction history layout
                    setContentView(R.layout.activity_transactions)
                    recyclerView = findViewById(R.id.transactions_recycler_view)
                    recyclerView.layoutManager = LinearLayoutManager(this)
                    recyclerView.adapter = TransactionAdapter(transactionList)
                    loadTransactions() // Reload transactions to include the new one
                } else {
                    // Handle save failure
                    Toast.makeText(this, "Failed to add transaction.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    // Method to load transactions from the database
    private fun loadTransactions() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("transactions")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                transactionList.clear() // Clear the list before adding new data
                for (snapshot in dataSnapshot.children) {
                    val transaction = snapshot.getValue(Transaction::class.java)
                    if (transaction != null) {
                        transactionList.add(transaction)
                    }
                }
                recyclerView.adapter?.notifyDataSetChanged() // Notify adapter about data changes
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
                Toast.makeText(this@TransactionsActivity, "Failed to load transactions.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Data class to represent a Transaction
    data class Transaction(
        val title: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val type: String = "",
        val toFrom: String = "",
        val category: String = "",
        val description: String = ""
    )

    // Adapter class for RecyclerView
    inner class TransactionAdapter(private val transactions: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

        // ViewHolder class to hold the views for each item
        inner class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val titleView: TextView = view.findViewById(R.id.transactionTitle)
            private val amountView: TextView = view.findViewById(R.id.transactionAmount)
            private val dateView: TextView = view.findViewById(R.id.transactionDate)

            // Method to bind data to views
            fun bind(transaction: Transaction) {
                titleView.text = transaction.title
                amountView.text = "$${transaction.amount}"
                dateView.text = transaction.date
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            // Inflate the layout for a single transaction item
            val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
            return TransactionViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            // Bind the transaction data to the ViewHolder
            val transaction = transactions[position]
            holder.bind(transaction)
        }

        override fun getItemCount() = transactions.size
    }
}
