package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton

class PaymentVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_verification)

        val status = intent.getStringExtra("STATUS") ?: "FAILURE"
        val orderId = intent.getStringExtra("ORDER_ID") ?: "N/A"
        val transactionId = intent.getStringExtra("TRANSACTION_ID")
        val courseId = intent.getStringExtra("COURSE_ID") ?: ""

        setupStatusBar()
        setupUI(status, orderId, transactionId, courseId)
    }

    private fun setupStatusBar() {
        // Set status bar color to match background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
    }

    private fun setupUI(status: String, orderId: String, transactionId: String?, courseId: String) {
        val lottieAnimation = findViewById<LottieAnimationView>(R.id.lottieAnimation)
        val tvStatusTitle = findViewById<TextView>(R.id.tvStatusTitle)
        val tvStatusMessage = findViewById<TextView>(R.id.tvStatusMessage)
        val tvOrderId = findViewById<TextView>(R.id.tvOrderId)
        val tvTransactionId = findViewById<TextView>(R.id.tvTransactionId)
        val layoutTransactionId = findViewById<LinearLayout>(R.id.layoutTransactionId)
        val btnPrimary = findViewById<MaterialButton>(R.id.btnPrimary)
        val btnSecondary = findViewById<MaterialButton>(R.id.btnSecondary)

        tvOrderId.text = orderId

        if (transactionId != null) {
            layoutTransactionId.visibility = View.VISIBLE
            tvTransactionId.text = transactionId
        } else {
            layoutTransactionId.visibility = View.GONE
        }

        if (status == "SUCCESS") {
            // Success animation
            lottieAnimation.setAnimation(R.raw.success)
            lottieAnimation.playAnimation()
            
            tvStatusTitle.text = "Payment Successful!"
            tvStatusTitle.setTextColor(getColor(R.color.colorSuccess))
            tvStatusMessage.text = "Your order has been placed successfully. You can now access the course content and start learning!"
            
            btnPrimary.text = "Go to Course"
            btnPrimary.setOnClickListener {
                // Navigate to Course Content (or Home for now)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        } else {
            // Failure animation
            lottieAnimation.setAnimation(R.raw.animation_b)
            lottieAnimation.playAnimation()
            
            tvStatusTitle.text = "Payment Failed"
            tvStatusTitle.setTextColor(getColor(R.color.colorError))
            tvStatusMessage.text = "Unfortunately, your payment could not be processed. Please check your payment details and try again."
            
            btnPrimary.text = "Retry Payment"
            btnPrimary.setOnClickListener {
                finish() // Go back to Checkout
            }
        }

        btnSecondary.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
