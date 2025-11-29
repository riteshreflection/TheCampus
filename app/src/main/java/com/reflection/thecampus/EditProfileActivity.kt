package com.reflection.thecampus

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etMobileNumber: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        setupToolbar()
        setupViews()
        loadUserProfile()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etMobileNumber = findViewById(R.id.etMobileNumber)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // Set email from Firebase Auth
        etEmail.setText(auth.currentUser?.email ?: "")

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("userProfiles")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.getValue(UserProfile::class.java)
                profile?.let {
                    etFullName.setText(it.fullName)
                    etMobileNumber.setText(it.mobileNumber)
                }
            }
            .addOnFailureListener { exception ->
                timber.log.Timber.e(exception, "Failed to load profile")
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val fullName = etFullName.text.toString().trim()
        val mobileNumber = etMobileNumber.text.toString().trim()
        val email = etEmail.text.toString().trim()

        // Validation using InputValidator
        val nameError = com.reflection.thecampus.utils.InputValidator.getNameErrorMessage(fullName)
        if (nameError !=null) {
            etFullName.error = nameError
            etFullName.requestFocus()
            return
        }

        val phoneError = com.reflection.thecampus.utils.InputValidator.getPhoneErrorMessage(mobileNumber)
        if (phoneError != null) {
            etMobileNumber.error = phoneError
            etMobileNumber.requestFocus()
            return
        }
        
        val emailError = com.reflection.thecampus.utils.InputValidator.getEmailErrorMessage(email)
        if (emailError != null) {
            etEmail.error = emailError
            etEmail.requestFocus()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        val profile = UserProfile(
            userId = userId,
            email = email,
            fullName = fullName,
            mobileNumber = mobileNumber,
            profilePictureUrl = "",
            createdAt = System.currentTimeMillis()
        )

        // Save to Firebase
        database.getReference("userProfiles")
            .child(userId)
            .setValue(profile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { exception ->
                timber.log.Timber.e(exception, "Failed to save profile")
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }
}
