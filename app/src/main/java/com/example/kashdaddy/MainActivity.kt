package com.example.kashdaddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
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

// Login and Registration in Android using Firebase in Kotlin
// https://www.geeksforgeeks.org/login-and-registration-in-android-using-firebase-in-kotlin/
// ayushpandey3july
// https://www.geeksforgeeks.org/user/ayushpandey3july/contributions/?itm_source=geeksforgeeks&itm_medium=article_author&itm_campaign=auth_user

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

        // Enable Firebase persistence before any other Firebase usage
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        database = FirebaseDatabase.getInstance().reference

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()

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

        // Check if "Remember Me" is enabled and user is already logged in
        if (sharedPreferences.getBoolean("rememberMe", false)) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                navigateToHome()
                finish()
                return
            }
        }

        setContentView(R.layout.activity_login)

        // Google Sign-In configuration
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

        // Set up Login button click listener
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
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
        if (isNetworkAvailable()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        user?.let {
                            saveLoginStateLocally(it.uid)
                            updateUI(user)
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        } else {
            // If offline, check stored user data for offline login
            val savedUserId = sharedPreferences.getString("userId", null)
            if (savedUserId != null) {
                navigateToHome()
            } else {
                Toast.makeText(this, "No network available and no offline data.", Toast.LENGTH_SHORT).show()
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
                    user?.let {
                        saveLoginStateLocally(it.uid)
                        updateUI(user)
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun saveLoginStateLocally(userId: String) {
        val rememberMe = rememberMeCheckBox.isChecked
        val editor = sharedPreferences.edit()
        editor.putBoolean("rememberMe", rememberMe)
        editor.putString("userId", userId)  // Save user ID locally
        editor.apply()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            navigateToHome()
        } else {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun fetchUserData(userId: String) {
        database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(RegisterActivity.User::class.java)
                user?.let {
                    // Use this data for offline purposes, store it locally if needed
                }
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to fetch user data", exception)
        }
    }

    private fun syncUserDataIfNeeded() {
        if (isNetworkAvailable()) {
            val userId = sharedPreferences.getString("userId", null)
            if (userId != null) {
                fetchUserData(userId)
            }
        }
    }
}



class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var btnRegister: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    private val RC_SIGN_IN = 9001
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()


        // Initialize SharedPreferences for local storage
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Initialize register button
        btnRegister = findViewById(R.id.btn_register)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Use your web client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Google Sign-In Button
        val googleSignInButton: SignInButton = findViewById(R.id.sign_in_button)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Set up Register button click listener
        btnRegister.setOnClickListener {
            val emailEditText: EditText = findViewById(R.id.et_email)
            val passwordEditText: EditText = findViewById(R.id.et_password)
            val nameEditText: EditText = findViewById(R.id.et_name)
            val surnameEditText: EditText = findViewById(R.id.et_surname)
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && surname.isNotEmpty()) {
                registerUser(email, password, name, surname)
            } else {
                Toast.makeText(this, "Please enter all fields.", Toast.LENGTH_SHORT).show()
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
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
                    // Save user to database if needed
                    user?.let { saveUserToDatabase(it, "Google User", "Google Surname") }
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun registerUser(email: String, password: String, name: String, surname: String) {
        if (isNetworkAvailable()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserToDatabase(user, name, surname)
                        updateUI(user)
                    } else {
                        Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        } else {
            Toast.makeText(this, "No internet connection. Data will be synced later.", Toast.LENGTH_SHORT).show()
            saveOfflineUser(email, password, name, surname)
            updateUI(null)
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser?, name: String, surname: String) {
        user?.let {
            val userId = it.uid
            val userData = User(it.email, name, surname)
            database.child("users").child(userId).setValue(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "User data saved successfully.")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to save user data", exception)
                }
        }
    }

    private fun saveOfflineUser(email: String, password: String, name: String, surname: String) {
        val editor = sharedPreferences.edit()
        editor.putString("offlineEmail", email)
        editor.putString("offlinePassword", password)
        editor.putString("offlineName", name)
        editor.putString("offlineSurname", surname)
        editor.apply()

        // Sync user data once the connection is available
        syncOfflineData()
    }

    private fun syncOfflineData() {
        if (isNetworkAvailable()) {
            val email = sharedPreferences.getString("offlineEmail", null)
            val password = sharedPreferences.getString("offlinePassword", null)
            val name = sharedPreferences.getString("offlineName", null)
            val surname = sharedPreferences.getString("offlineSurname", null)

            if (email != null && password != null && name != null && surname != null) {
                registerUser(email, password, name, surname)
                clearOfflineData()
            }
        }
    }

    private fun clearOfflineData() {
        val editor = sharedPreferences.edit()
        editor.remove("offlineEmail")
        editor.remove("offlinePassword")
        editor.remove("offlineName")
        editor.remove("offlineSurname")
        editor.apply()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }

    data class User(val email: String?, val name: String, val surname: String)
}

