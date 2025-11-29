package com.reflection.thecampus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PaymentVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_verification)

        // Set status bar color
        window.statusBarColor = getColor(android.R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val status = intent.getStringExtra("STATUS") ?: "FAILURE"
        val orderId = intent.getStringExtra("ORDER_ID") ?: "N/A"
        val transactionId = intent.getStringExtra("TRANSACTION_ID")
        val courseId = intent.getStringExtra("COURSE_ID") ?: ""

        setupUI(status, orderId, transactionId, courseId)
    }

    private fun setupUI(status: String, orderId: String, transactionId: String?, courseId: String) {
        val ivStatusIcon = findViewById<ImageView>(R.id.ivStatusIcon)
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
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
            ivStatusIcon.setColorFilter(getColor(R.color.colorSuccess))
            tvStatusTitle.text = "Payment Successful!"
            tvStatusMessage.text = "Your order has been placed successfully. You can now access the course."
            
            btnPrimary.text = "Go to Course"
            btnPrimary.setOnClickListener {
                // Navigate to Course Content (or Home for now)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_close) // Reusing close icon as error icon
            ivStatusIcon.setColorFilter(getColor(R.color.colorError))
            tvStatusTitle.text = "Payment Failed"
            tvStatusMessage.text = "Something went wrong with your payment. Please try again."
            
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
