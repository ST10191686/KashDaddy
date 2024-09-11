package com.example.kashdaddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.common.SignInButton

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private val RC_SIGN_IN = 9001
    private val TAG = "MainActivity"
    private lateinit var btnLogin: Button
    private lateinit var rememberMeCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Check if "Remember Me" is enabled
        if (sharedPreferences.getBoolean("rememberMe", false)) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is already logged in, navigate to home screen
                navigateToHome()
                finish()
                return
            }
        }

        setContentView(R.layout.activity_login)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize buttons and other UI components
        btnLogin = findViewById(R.id.btn_login)
        val registerButton: Button = findViewById(R.id.btn_register)
        val googleSignInButton: SignInButton = findViewById(R.id.sign_in_button)
        rememberMeCheckBox = findViewById(R.id.cb_remember_me)

        // Set up Google Sign-In button click listener
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Set up Register button click listener to navigate to RegisterActivity
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Set up Login button click listener (traditional email/password login)
        btnLogin.setOnClickListener {
            val emailEditText: EditText = findViewById(R.id.et_email)
            val passwordEditText: EditText = findViewById(R.id.et_password)
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginWithEmailPassword(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.w(TAG, "ID Token is null")
                    updateUI(null)
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                updateUI(null)
            }
        }
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    // Save "Remember Me" state
                    saveRememberMeState()
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    // Save "Remember Me" state
                    saveRememberMeState()
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun saveRememberMeState() {
        val rememberMe = rememberMeCheckBox.isChecked
        val editor = sharedPreferences.edit()
        editor.putBoolean("rememberMe", rememberMe)
        editor.apply()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in, navigate to home screen or main content
            navigateToHome()
        } else {
            // User is signed out or login failed
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHome() {
        // Navigate to the home or dashboard activity
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize register button
        btnRegister = findViewById(R.id.btn_register)

        // Set up Register button click listener
        btnRegister.setOnClickListener {
            val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
            val password = findViewById<EditText>(R.id.et_password).text.toString().trim()
            val name = findViewById<EditText>(R.id.et_name).text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                registerUser(email, password, name)
            } else {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToDatabase(user, name)
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    finish()  // Go back to login screen after registration
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(user: FirebaseUser?, name: String) {
        if (user != null) {
            val userId = user.uid
            val userEmail = user.email ?: "Unknown"
            val userMap = mapOf(
                "name" to name,
                "email" to userEmail
            )
            database.child("users").child(userId).setValue(userMap)
                .addOnSuccessListener {
                    Log.d("RegisterActivity", "User data saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.w("RegisterActivity", "Failed to save user data", e)
                }
        }
    }
}




