package com.reflection.thecampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class SuspendedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suspended)
        
        // Set status bar color to match surface
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data
        
        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        val lottieView = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieSuspended)
        lottieView.setAnimation(R.raw.animation_b)
        lottieView.playAnimation()

        val btnContactSupport = findViewById<Button>(R.id.btnContactSupport)
        btnContactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@thecampus.in")
                putExtra(Intent.EXTRA_SUBJECT, "Account Suspension Appeal")
            }
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        // Prevent going back
        super.onBackPressed()
        finishAffinity()
    }
}
