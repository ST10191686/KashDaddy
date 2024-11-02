package com.example.kashdaddy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging

// Login and Registration in Android using Firebase in Kotlin
// https://www.geeksforgeeks.org/login-and-registration-in-android-using-firebase-in-kotlin/
// ayushpandey3july
// https://www.geeksforgeeks.org/user/ayushpandey3july/contributions/?itm_source=geeksforgeeks&itm_medium=article_author&itm_campaign=auth_user


// Biometric Authentication in Android Kotlin…
// https://saqibvnb.medium.com/biometric-authentication-in-android-kotlin-2178cd227afb
// Saqib Ahmed
// https://saqibvnb.medium.com

// BalckBox AI
// how to implement biometric fingerprint to login
// https://www.blackbox.ai/chat/lbgB1zN

class MainActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnLogin: Button
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricManager: BiometricManager
    private val RC_SIGN_IN = 9001

    private val KEY_NAME = "KashDaddyKey"
    private val TAG = "MainActivity"
    private lateinit var keyStore: KeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Set up Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI elements
        btnLogin = findViewById(R.id.btn_login)
        rememberMeCheckBox = findViewById(R.id.cb_remember_me)
        val googleSignInButton: SignInButton = findViewById(R.id.sign_in_button)
        val fingerprintIcon: ImageView = findViewById(R.id.fingerprint_icon)
        val registerButton: Button = findViewById(R.id.btn_register)

        // Set up biometric components
        biometricManager = BiometricManager.from(this)
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Authentication succeeded!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToHome()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        // Initialize KeyStore
        keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        // Set up button listeners
        googleSignInButton.setOnClickListener { signInWithGoogle() }
        btnLogin.setOnClickListener { handleLogin() }
        fingerprintIcon.setOnClickListener { handleBiometricPrompt() }

        // Register button click listener to navigate to RegisterActivity
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString().trim()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveCredentials(email, password)
                    navigateToHome()
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
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
                val account = task.result
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveCredentials(auth.currentUser!!.email!!, "google_password_placeholder")
                navigateToHome()
            } else {
                Toast.makeText(this, "Authentication with Google failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleBiometricPrompt() {
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for KashDaddy")
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Use account password")
                    .build()
            )
        } else {
            Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_NAME)) {
            val keyGen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            keyGen.init(
                KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false) // Authentication not strictly required for demo purposes
                    .build()
            )
            return keyGen.generateKey()
        }
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    private fun encryptData(data: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encryptionIv = cipher.iv
        sharedPreferences.edit().putString("iv", android.util.Base64.encodeToString(encryptionIv, android.util.Base64.DEFAULT)).apply()
        return cipher.doFinal(data.toByteArray())
    }

    private fun saveCredentials(email: String, password: String) {
        val encryptedEmail = encryptData(email)
        val encryptedPassword = encryptData(password)
        sharedPreferences.edit()
            .putString("email", android.util.Base64.encodeToString(encryptedEmail, android.util.Base64.DEFAULT))
            .putString("password", android.util.Base64.encodeToString(encryptedPassword, android.util.Base64.DEFAULT))
            .apply()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
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

