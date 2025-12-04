package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var userProfile: UserProfile? = null

    companion object {
        const val REQUEST_EDIT_PROFILE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dynamic theme BEFORE super.onCreate to ensure it takes effect for all attributes
        com.reflection.thecampus.utils.ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_settings)

        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val btnLogout = findViewById<View>(R.id.btnLogout)
        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)

        // Set User Email
        val user = auth.currentUser
        tvUserEmail.text = user?.email ?: "Not Logged In"

        // Load user profile
        loadUserProfile(tvUserName)

        // Dark Mode Logic
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Theme Color Click
        val btnThemeColor = findViewById<View>(R.id.btnThemeColor)
        val tvCurrentTheme = findViewById<TextView>(R.id.tvCurrentTheme)
        
        // Display current theme
        val currentTheme = com.reflection.thecampus.utils.ThemeManager.getCurrentTheme(this)
        tvCurrentTheme.text = currentTheme.themeName
        
        btnThemeColor.setOnClickListener {
            showThemePickerDialog()
        }

        val profileCard = findViewById<CardView>(R.id.profileCard)
        profileCard?.setOnClickListener {
            openEditProfile()
        }
        
        // Test History Click
        findViewById<View>(R.id.btnTestHistory)?.setOnClickListener {
            startActivity(Intent(this, com.reflection.thecampus.ui.test.TestHistoryActivity::class.java))
        }
        
        // Analytics Click
        findViewById<View>(R.id.btnAnalytics)?.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }

        // Refer & Earn Click
        findViewById<View>(R.id.btnReferEarn)?.setOnClickListener {
            startActivity(Intent(this, com.reflection.thecampus.ui.referral.ReferralActivity::class.java))
        }

        // Logout Logic
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Legal & More Section Clicks
        findViewById<View>(R.id.btnTerms)?.setOnClickListener { openUrl("https://app.thecampus.in/terms-and-conditions") }
        findViewById<View>(R.id.btnPrivacy)?.setOnClickListener { openUrl("https://app.thecampus.in/privacy-policy") }
        findViewById<View>(R.id.btnContactUs)?.setOnClickListener { openUrl("https://app.thecampus.in/contact-us") }
        findViewById<View>(R.id.btnBlogs)?.setOnClickListener { openUrl("https://www.thecampus.in/blog") }
        findViewById<View>(R.id.btnDeleteAccount)?.setOnClickListener { openUrl("https://www.thecampus.in/delete-account") }
        
        findViewById<View>(R.id.btnAboutApp)?.setOnClickListener { showAppInfoDialog() }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Could not open link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppInfoDialog() {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val version = pInfo.versionName
        
        android.app.AlertDialog.Builder(this)
            .setTitle("The Campus")
            .setMessage("Version: $version\n\n© 2026 The Campus. All rights reserved.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadUserProfile(tvUserName: TextView) {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("userProfiles")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                userProfile = snapshot.getValue(UserProfile::class.java)
                
                if (userProfile == null || !userProfile!!.isComplete()) {
                    // Show incomplete profile warning
                    showIncompleteProfileWarning()
                    tvUserName.text = "Complete your profile"
                } else {
                    tvUserName.text = userProfile?.fullName ?: "User"
                }
            }
    }

    private fun showIncompleteProfileWarning() {
        val stubIncompleteProfile = findViewById<ViewStub>(R.id.stubIncompleteProfile)
        val inflatedView = stubIncompleteProfile?.inflate()
        
        inflatedView?.findViewById<MaterialButton>(R.id.btnCompleteProfile)?.setOnClickListener {
            openEditProfile()
        }
    }

    private fun openEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        startActivityForResult(intent, REQUEST_EDIT_PROFILE)
    }
    
    private fun showThemePickerDialog() {
        val dialog = com.reflection.thecampus.ui.dialogs.ThemePickerDialog.newInstance()
        dialog.setOnThemeSelectedListener { selectedTheme ->
            // Save the selected theme
            com.reflection.thecampus.utils.ThemeManager.saveTheme(this, selectedTheme)
            
            // Update the current theme display
            findViewById<TextView>(R.id.tvCurrentTheme).text = selectedTheme.themeName
            
            // Show restart dialog
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Theme Changed")
                .setMessage("The app needs to restart to apply the new theme color. Restart now?")
                .setPositiveButton("Restart") { _, _ ->
                    // Restart the app
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                    Runtime.getRuntime().exit(0)
                }
                .setNegativeButton("Later") { _, _ ->
                    // User chose to restart later, show a toast
                    android.widget.Toast.makeText(
                        this,
                        "Theme will be applied on next app restart",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        }
        dialog.show(supportFragmentManager, com.reflection.thecampus.ui.dialogs.ThemePickerDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == RESULT_OK) {
            // Reload the activity to refresh profile data
            recreate()
        }
    }
}
