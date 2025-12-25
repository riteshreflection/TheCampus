package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import com.reflection.thecampus.UserProfile

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var etReferralCode: com.google.android.material.textfield.TextInputEditText
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var btnSignup: android.widget.Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val etName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etEmail = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val tilName = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilName)
        val tilEmail = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)
        etReferralCode = findViewById(R.id.etReferralCode)
        btnSignup = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)
        val tvLogin = findViewById<android.widget.TextView>(R.id.tvLogin)

        // Clear errors when user types
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilName.error = null
        }
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }

        // Handle Deep Link
        val data: android.net.Uri? = intent.data
        if (data != null && data.getQueryParameter("ref") != null) {
            etReferralCode.setText(data.getQueryParameter("ref"))
        }

        btnSignup.setOnClickListener {
            // Clear previous errors
            tilName.error = null
            tilEmail.error = null
            tilPassword.error = null

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val referralCode = etReferralCode.text.toString().trim()

            // Validate inputs
            var hasError = false

            if (name.isEmpty()) {
                tilName.error = "Name is required"
                hasError = true
            }

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                hasError = true
            }

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                hasError = true
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            setLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid ?: ""
                        
                        // Save Token
                        com.reflection.thecampus.utils.FCMManager.saveToken(this, userId)
                        
                        // Create Session
                        com.reflection.thecampus.utils.SessionManager.createSession(this, userId)

                        val userProfile = UserProfile(userId, name, email)
                        
                        // Handle Referral if code exists
                        if (referralCode.isNotEmpty()) {
                            processReferral(userId, referralCode, userProfile)
                        } else {
                            saveUserAndFinish(userId, userProfile)
                        }
                    } else {
                        setLoading(false)
                        // Show user-friendly error message
                        val errorMessage = com.reflection.thecampus.utils.AuthErrorHandler.getErrorMessage(task.exception)
                        
                        // Determine which field to show error on
                        val message = task.exception?.message?.lowercase() ?: ""
                        when {
                            message.contains("password") -> tilPassword.error = errorMessage
                            message.contains("email") -> tilEmail.error = errorMessage
                            else -> tilEmail.error = errorMessage // Default to email field
                        }
                    }
                }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = android.view.View.VISIBLE
            btnSignup.text = ""
            btnSignup.isEnabled = false
        } else {
            progressBar.visibility = android.view.View.GONE
            btnSignup.text = getString(R.string.signup_button)
            btnSignup.isEnabled = true
        }
    }

    private fun processReferral(newUserId: String, referralCode: String, userProfile: UserProfile) {
        val usersRef = database.getReference("users")
        
        // Find Referrer by Code
        usersRef.orderByChild("referral/code").equalTo(referralCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Referrer Found
                    val referrerSnapshot = snapshot.children.iterator().next()
                    val referrerId = referrerSnapshot.key
                    
                    if (referrerId != null) {
                        // 1. Create Referral Record
                        val referralRecord = mapOf(
                            "signupAt" to System.currentTimeMillis(),
                            "status" to "pending_enrollment"
                        )
                        database.getReference("referrals").child(referrerId).child("referees").child(newUserId).setValue(referralRecord)

                        // 2. Update New User with ReferredBy
                        database.getReference("users").child(newUserId).child("referral").child("referredBy").setValue(referralCode)
                        saveUserAndFinish(newUserId, userProfile)
                    } else {
                        // Should not happen, but fallback
                        saveUserAndFinish(newUserId, userProfile)
                    }
                } else {
                    // Invalid Code, proceed without referral
                    saveUserAndFinish(newUserId, userProfile)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                saveUserAndFinish(newUserId, userProfile)
            }
        })
    }

    private fun saveUserAndFinish(userId: String, userProfile: UserProfile) {
        database.getReference("users").child(userId).setValue(userProfile)
            .addOnCompleteListener {
                setLoading(false)
                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }
}
