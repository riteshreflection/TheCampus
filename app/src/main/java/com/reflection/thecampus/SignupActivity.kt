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
        etReferralCode = findViewById(R.id.etReferralCode)
        btnSignup = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)
        val tvLogin = findViewById<android.widget.TextView>(R.id.tvLogin)

        // Handle Deep Link
        val data: android.net.Uri? = intent.data
        if (data != null && data.getQueryParameter("ref") != null) {
            etReferralCode.setText(data.getQueryParameter("ref"))
        }

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val referralCode = etReferralCode.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                        Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
