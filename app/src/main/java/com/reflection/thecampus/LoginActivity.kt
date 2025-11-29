package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val database = FirebaseDatabase.getInstance()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etEmail = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoogleSignIn)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)
        val ivLogo = findViewById<android.widget.ImageView>(R.id.ivLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Show Loading
                progressBar.visibility = android.view.View.VISIBLE
                btnLogin.text = ""
                btnLogin.isEnabled = false

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            // Create Session
                            com.reflection.thecampus.utils.SessionManager.createSession(this, auth.currentUser!!.uid)
                            
                            // Save FCM Token
                            com.reflection.thecampus.utils.FCMManager.saveToken(this, auth.currentUser!!.uid)
                            
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // Hide Loading
                            progressBar.visibility = android.view.View.GONE
                            btnLogin.text = getString(R.string.login_button)
                            btnLogin.isEnabled = true
                            
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val btnGoogleSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoogleSignIn)
        
        progressBar.visibility = android.view.View.VISIBLE
        btnGoogleSignIn.isEnabled = false

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: ""
                    
                    // Check if user profile exists
                    database.getReference("users").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    // Create new user profile
                                    val userProfile = UserProfile(
                                        userId = userId,
                                        email = user?.email ?: "",
                                        fullName = user?.displayName ?: "",
                                        profilePictureUrl = user?.photoUrl?.toString() ?: "",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    
                                    database.getReference("users").child(userId).setValue(userProfile)
                                        .addOnCompleteListener {
                                            completeGoogleSignIn(userId)
                                        }
                                } else {
                                    // Existing user
                                    completeGoogleSignIn(userId)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                progressBar.visibility = android.view.View.GONE
                                btnGoogleSignIn.isEnabled = true
                                Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                } else {
                    progressBar.visibility = android.view.View.GONE
                    btnGoogleSignIn.isEnabled = true
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun completeGoogleSignIn(userId: String) {
        // Create Session
        com.reflection.thecampus.utils.SessionManager.createSession(this, userId)
        
        // Save FCM Token
        com.reflection.thecampus.utils.FCMManager.saveToken(this, userId)
        
        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showForgotPasswordDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        builder.setMessage("Enter your email address to receive a password reset link")

        // Create custom view for the dialog
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "Email"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val marginInPx = (24 * resources.displayMetrics.density).toInt()
        params.leftMargin = marginInPx
        params.rightMargin = marginInPx
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Send Reset Link") { dialog, _ ->
            val email = input.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Show progress dialog
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage("Sending reset link...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            // Send password reset email
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()

                    if (task.isSuccessful) {
                        // Show success dialog
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Email Sent!")
                            .setMessage("Password reset link has been sent to $email. Please check your inbox.")
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .setIcon(android.R.drawable.ic_dialog_email)
                            .show()

                        timber.log.Timber.d("Password reset email sent to: $email")
                    } else {
                        // Show error dialog
                        val errorMessage = when {
                            task.exception?.message?.contains("no user record", ignoreCase = true) == true ->
                                "No account found with this email address"
                            task.exception?.message?.contains("badly formatted", ignoreCase = true) == true ->
                                "Invalid email format"
                            else -> task.exception?.message ?: "Failed to send reset link"
                        }

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage(errorMessage)
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()

                        timber.log.Timber.e("Password reset failed: ${task.exception?.message}")
                    }
                }

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}
