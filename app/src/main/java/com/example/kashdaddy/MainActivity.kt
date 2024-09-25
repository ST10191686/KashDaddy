package com.example.kashdaddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.common.SignInButton
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private val RC_SIGN_IN = 9001
    private val TAG = "MainActivity"
    private lateinit var btnLogin: Button
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricManager: BiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Initialize Biometric components
        executor = ContextCompat.getMainExecutor(this)
        biometricManager = BiometricManager.from(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Check if user is already authenticated
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    navigateToHome()
                } else {
                    Toast.makeText(applicationContext, "Please log in with your password.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        // Check if "Remember Me" is enabled and navigate accordingly
        if (sharedPreferences.getBoolean("rememberMe", false)) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
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
        val fingerprintIcon = findViewById<ImageView>(R.id.fingerprint_icon)

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

        // Set up fingerprint icon click listener
        fingerprintIcon.setOnClickListener {
            if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                showBiometricPrompt()
            } else {
                Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for KashDaddy")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build())
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

    private fun fetchUserData(userId: String) {
        database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(RegisterActivity.User::class.java)
                user?.let {
                    // Use it to display on settings page
                    // For example, you might store it in shared preferences or directly pass it to the settings activity
                }
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to fetch user data", exception)
        }
    }

}


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize register button
        btnRegister = findViewById(R.id.btn_register)

        // Set up Register button click listener
        btnRegister.setOnClickListener {
            val emailEditText: EditText = findViewById(R.id.et_email)
            val passwordEditText: EditText = findViewById(R.id.et_password)
            val nameEditText: EditText = findViewById(R.id.et_name) // Add name EditText
            val surnameEditText: EditText = findViewById(R.id.et_surname) // Add surname EditText
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim() // Get name
            val surname = surnameEditText.text.toString().trim() // Get surname

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && surname.isNotEmpty()) {
                registerUser(email, password, name, surname)
            } else {
                Toast.makeText(this, "Please enter all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, name: String, surname: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "createUserWithEmail:success")
                    val user = auth.currentUser
                    saveUserToDatabase(user, name, surname) // Pass name and surname
                    updateUI(user)
                } else {
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun saveUserToDatabase(user: FirebaseUser?, name: String, surname: String) {
        user?.let {
            val userId = it.uid
            val userData = User(it.email, name, surname) // Update User data class
            database.child("users").child(userId).setValue(userData)
                .addOnSuccessListener {
                    Log.d("RegisterActivity", "User data saved successfully.")
                }
                .addOnFailureListener { exception ->
                    Log.e("RegisterActivity", "Failed to save user data", exception)
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
        }
    }

    data class User(val email: String?, val name: String, val surname: String) // Update data class
}


