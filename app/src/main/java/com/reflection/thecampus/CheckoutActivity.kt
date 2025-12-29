package com.reflection.thecampus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.reflection.thecampus.data.model.Offer
import com.reflection.thecampus.data.model.PriceDetails
import com.reflection.thecampus.utils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CheckoutActivity : AppCompatActivity(), CFCheckoutResponseCallback {

    private lateinit var viewModel: CourseDetailViewModel
    private var courseId: String = ""
    private var currentCourse: Course? = null
    private var userProfile: UserProfile? = null
    private var paymentLoadingBottomSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    private lateinit var cbPolicies: CheckBox
    private lateinit var btnProceedToPayment: MaterialButton
    
    private lateinit var tilCoupon: TextInputLayout
    private lateinit var etCoupon: TextInputEditText
    private lateinit var btnApplyCoupon: MaterialButton
    private lateinit var layoutAppliedCoupon: LinearLayout
    private lateinit var tvAppliedCouponCode: TextView
    private lateinit var tvAppliedCouponDesc: TextView
    private lateinit var ivRemoveCoupon: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check authentication first - redirect if not logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setContentView(R.layout.activity_checkout)

        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        } catch (e: CFException) {
            e.printStackTrace()
        }

        // Set status bar color to match premium background
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        viewModel = ViewModelProvider(this)[CourseDetailViewModel::class.java]

        // Get course ID from intent
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        
        if (courseId.isEmpty()) {
            Toast.makeText(this, "Invalid course", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupViews()
        setupCoupons()
        loadUserProfile()
        loadCourseData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        cbPolicies = findViewById(R.id.cbPolicies)
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment)
        
        // Force MaterialButton to use custom background
        btnProceedToPayment.setBackgroundResource(R.drawable.bg_payment_button_premium)
        btnProceedToPayment.backgroundTintList = null
        
        tilCoupon = findViewById(R.id.tilCoupon)
        etCoupon = findViewById(R.id.etCoupon)
        btnApplyCoupon = findViewById(R.id.btnApplyCoupon)
        layoutAppliedCoupon = findViewById(R.id.layoutAppliedCoupon)
        tvAppliedCouponCode = findViewById(R.id.tvAppliedCouponCode)
        tvAppliedCouponDesc = findViewById(R.id.tvAppliedCouponDesc)
        ivRemoveCoupon = findViewById(R.id.ivRemoveCoupon)

        // Setup Policy Links
        setupPolicyLinks()

        // Enable button only when checkbox is checked
        cbPolicies.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        btnProceedToPayment.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showPaymentLoadingBottomSheet()
            proceedToPayment()
        }
        
        btnApplyCoupon.setOnClickListener {
            val code = etCoupon.text.toString().trim()
            if (code.isNotEmpty()) {
                viewModel.applyCoupon(code)
            }
        }
        
        ivRemoveCoupon.setOnClickListener {
            viewModel.removeCoupon()
            etCoupon.setText("")
            layoutAppliedCoupon.visibility = View.GONE
            tilCoupon.visibility = View.VISIBLE
            btnApplyCoupon.visibility = View.VISIBLE
        }
    }

    private fun updateButtonState() {
        val isEnabled = cbPolicies.isChecked
        btnProceedToPayment.isEnabled = isEnabled
        
        // Explicitly set background to ensure it shows properly
        if (isEnabled) {
            btnProceedToPayment.setBackgroundResource(R.drawable.bg_payment_button_premium)
            // Just add entrance animation, no continuous pulsing
            btnProceedToPayment.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        } else {
            btnProceedToPayment.clearAnimation()
            // Reset to static background when disabled
            btnProceedToPayment.setBackgroundResource(R.drawable.bg_payment_button_premium)
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("userProfiles")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                userProfile = snapshot.getValue(UserProfile::class.java)
                
                // Validate profile completeness
                if (userProfile == null || !isProfileComplete(userProfile)) {
                    showProfileIncompleteDialog()
                }
            }
            .addOnFailureListener {
                // If profile fetch fails, show alert
                showProfileIncompleteDialog()
            }
    }
    
    
    private fun isProfileComplete(profile: UserProfile?): Boolean {
        if (profile == null) return false
        
        // Check required fields - UserProfile has: userId, email, fullName, mobileNumber
        return profile.fullName.isNotBlank() &&
               profile.email.isNotBlank() &&
               profile.mobileNumber.isNotBlank()
    }


    private fun setupCoupons() {
        val adapter = CouponAdapter { offer ->
            viewModel.applyCoupon(offer.couponCode)
        }
        val rvCoupons = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCoupons)
        rvCoupons.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvCoupons.adapter = adapter

        viewModel.offers.observe(this) { offers ->
            Log.d("CheckoutActivity", "Received offers: ${offers.size}")
            offers.forEach { Log.d("CheckoutActivity", "Offer: ${it.couponCode}, Public: ${it.isPublic}, Status: ${it.status}") }

            // Filter offers locally for display if needed, or rely on ViewModel to provide filtered list
            // For now, show all public active offers
            val publicOffers = offers.filter { it.isPublic && it.status == "active" }
            Log.d("CheckoutActivity", "Filtered public offers: ${publicOffers.size}")
            
            adapter.submitList(publicOffers)
            
            if (publicOffers.isEmpty()) {
                rvCoupons.visibility = View.GONE
            } else {
                rvCoupons.visibility = View.VISIBLE
            }
        }
    }

    private fun loadCourseData() {
        viewModel.loadCourse(courseId)

        viewModel.course.observe(this) { course ->
            course?.let {
                currentCourse = it
                displayCourseData(it)
            }
        }
        
        viewModel.priceDetails.observe(this) { priceDetails ->
            updatePriceUI(priceDetails)
        }
        
        viewModel.couponValidationMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.orderCreated.observe(this) { order ->
            if (order != null) {
                initiatePayment(order.orderId, order.priceDetails.finalPrice)
            } else {
                Toast.makeText(this, "Failed to create order", Toast.LENGTH_SHORT).show()
                btnProceedToPayment.isEnabled = true
                btnProceedToPayment.text = "Proceed to Payment"
            }
        }
    }

    private fun displayCourseData(course: Course) {
        // Course Thumbnail
        val ivThumbnail = findViewById<ImageView>(R.id.ivCourseThumbnail)
        Glide.with(this)
            .load(course.pricing.thumbnailUrl)
            .placeholder(R.drawable.ic_book)
            .into(ivThumbnail)

        // Course Details
        findViewById<TextView>(R.id.tvCourseName).text = course.basicInfo.name
        findViewById<TextView>(R.id.tvCourseType).text = course.basicInfo.type
        findViewById<TextView>(R.id.tvCourseLevel).text = course.basicInfo.level
        findViewById<TextView>(R.id.tvLectures).text = "${course.schedule.totalLectures} Lectures"
        findViewById<TextView>(R.id.tvTests).text = "${course.schedule.totalTests} Tests"
    }

    private fun updatePriceUI(priceDetails: PriceDetails) {
        val tvOriginalPrice = findViewById<TextView>(R.id.tvOriginalPrice)
        val tvTotalPrice = findViewById<TextView>(R.id.tvTotalPrice)
        
        // Animate price changes
        tvOriginalPrice.text = "₹${priceDetails.originalPrice.toInt()}"
        tvTotalPrice.text = "₹${priceDetails.finalPrice.toInt()}"
        
        // Add subtle scale animation to total price
        tvTotalPrice.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                tvTotalPrice.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()

        // Discount section
        val layoutDiscount = findViewById<LinearLayout>(R.id.layoutDiscount)
        if (priceDetails.siteDiscount > 0) {
            layoutDiscount.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvDiscountLabel).text = "Discount"
            findViewById<TextView>(R.id.tvDiscountAmount).text = "- ₹${priceDetails.siteDiscount.toInt()}"
        } else {
            layoutDiscount.visibility = View.GONE
        }
        
        // Subtotal
        findViewById<TextView>(R.id.tvSubtotal).text = "₹${priceDetails.subtotal.toInt()}"

        // Coupon Discount
        val layoutCouponDiscount = findViewById<LinearLayout>(R.id.layoutCouponDiscount)
        if (priceDetails.couponDiscount > 0) {
            layoutCouponDiscount.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvCouponDiscountLabel).text = "Coupon (${priceDetails.couponCode})"
            findViewById<TextView>(R.id.tvCouponDiscountAmount).text = "- ₹${priceDetails.couponDiscount.toInt()}"
        } else {
            layoutCouponDiscount.visibility = View.GONE
        }

        // Tax
        val layoutTax = findViewById<LinearLayout>(R.id.layoutTax)
        if (priceDetails.taxDetails != null && priceDetails.taxDetails.taxAmount > 0) {
            layoutTax.visibility = View.VISIBLE
            val taxLabel = "${priceDetails.taxDetails.taxName} (${priceDetails.taxDetails.taxRate}%)"
            findViewById<TextView>(R.id.tvTaxLabel).text = taxLabel
            findViewById<TextView>(R.id.tvTaxAmount).text = "₹${priceDetails.taxDetails.taxAmount.toInt()}"
        } else {
            layoutTax.visibility = View.GONE
        }
        
        // Coupon UI with animation
        if (priceDetails.couponCode != null) {
            layoutAppliedCoupon.visibility = View.VISIBLE
            tilCoupon.visibility = View.GONE
            btnApplyCoupon.visibility = View.GONE
            
            tvAppliedCouponCode.text = priceDetails.couponCode
            tvAppliedCouponDesc.text = "₹${priceDetails.couponDiscount.toInt()} saved"
            
            // Add success animation
            layoutAppliedCoupon.alpha = 0f
            layoutAppliedCoupon.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            
            // Haptic feedback for success
            layoutAppliedCoupon.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        } else {
            layoutAppliedCoupon.visibility = View.GONE
            tilCoupon.visibility = View.VISIBLE
            btnApplyCoupon.visibility = View.VISIBLE
        }
    }

    private fun proceedToPayment() {
        // Check network first
        if (!isNetworkAvailable()) {
            Toast.makeText(
                this,
                "No internet connection. Please check your network.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // Check if profile is complete
        if (userProfile == null || !userProfile!!.isComplete()) {
            showProfileIncompleteDialog()
            return
        }

        makePayment()
    }

    private fun makePayment() {
        // Show loading
        btnProceedToPayment.isEnabled = false
        btnProceedToPayment.text = "Processing..."
        
        // Create Order first
        viewModel.createOrder("cashfree")
    }

    private fun initiatePayment(orderId: String, amount: Double) {
        val user = auth.currentUser ?: return
        val customerId = user.uid
        val customerPhone = userProfile?.mobileNumber ?: "9999999999"
        
        // Get applied coupon code (if any) from viewModel
        val appliedCouponCode = viewModel.priceDetails.value?.couponCode

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(BuildConfig.PAYMENT_API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000 // Add timeout
                conn.readTimeout = 15000
                conn.doOutput = true

                val jsonParam = JSONObject().apply {
                    put("courseId", courseId)
                    put("order_id", orderId)
                    put("customer_id", customerId)
                    put("customer_phone", customerPhone)
                    if (!appliedCouponCode.isNullOrEmpty()) {
                        put("couponCode", appliedCouponCode)
                    }
                    // Server will calculate amount based on courseId + couponCode
                }

                conn.outputStream.use { os ->
                    val input = jsonParam.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val paymentSessionId = jsonResponse.getString("payment_session_id")

                    withContext(Dispatchers.Main) {
                        launchCashfreePayment(paymentSessionId, orderId)
                    }
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CheckoutActivity,
                            "Payment failed: $errorResponse",
                            Toast.LENGTH_LONG
                        ).show()
                        resetPaymentButton()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CheckoutActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetPaymentButton()
                }
            }
        }
    }
    
    private fun resetPaymentButton() {
        btnProceedToPayment.isEnabled = true
        btnProceedToPayment.text = "Proceed to Payment"
    }

    private fun launchCashfreePayment(paymentSessionId: String, orderId: String) {
        try {
            val cfSession = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.PRODUCTION)
                .setPaymentSessionID(paymentSessionId)
                .setOrderId(orderId)
                .build()

            val cfTheme = CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                .setNavigationBarBackgroundColor("#98a7e2")
                .setNavigationBarTextColor("#ffffff")
                .build()

            val cfWebCheckoutPayment = CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                .setSession(cfSession)
                .setCFWebCheckoutUITheme(cfTheme)
                .build()

            CFPaymentGatewayService.getInstance().doPayment(this, cfWebCheckoutPayment)
        } catch (e: CFException) {
            e.printStackTrace()
            Toast.makeText(this, "Error launching payment", Toast.LENGTH_SHORT).show()
            btnProceedToPayment.isEnabled = true
            btnProceedToPayment.text = "Proceed to Payment"
        }
    }

    private fun setupPolicyLinks() {
        val tvPolicyText = findViewById<TextView>(R.id.tvPolicyText)

        val fullText = "I accept the Payment Policy and Refund Policy"
        val spannableString = SpannableString(fullText)
        
        // Make "Payment Policy" clickable
        val paymentPolicyStart = fullText.indexOf("Payment Policy")
        val paymentPolicyEnd = paymentPolicyStart + "Payment Policy".length
        
        val paymentPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thecampus.in/terms-and-conditions"))
                startActivity(intent)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = resources.getColor(R.color.colorPrimary, theme)
            }
        }
        spannableString.setSpan(paymentPolicySpan, paymentPolicyStart, paymentPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        // Make "Refund Policy" clickable
        val refundPolicyStart = fullText.indexOf("Refund Policy")
        val refundPolicyEnd = refundPolicyStart + "Refund Policy".length
        
        val refundPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thecampus.in/refund-and-cancellation-policy"))
                startActivity(intent)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = resources.getColor(R.color.colorPrimary, theme)
            }
        }
        spannableString.setSpan(refundPolicySpan, refundPolicyStart, refundPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvPolicyText.text = spannableString
        tvPolicyText.movementMethod = LinkMovementMethod.getInstance()
    }
    
    private fun showPaymentLoadingBottomSheet() {
        paymentLoadingBottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_payment_loading, null)
        paymentLoadingBottomSheet?.setContentView(view)
        paymentLoadingBottomSheet?.setCancelable(false)
        paymentLoadingBottomSheet?.show()
    }
    
    private fun dismissPaymentLoadingBottomSheet() {
        paymentLoadingBottomSheet?.dismiss()
        paymentLoadingBottomSheet = null
    }

    override fun onPaymentVerify(orderID: String?) {
        Log.d("CheckoutActivity", "Payment verified for order: $orderID")
        dismissPaymentLoadingBottomSheet()
        // Navigate to PaymentVerificationActivity
        val intent = Intent(this, PaymentVerificationActivity::class.java)
        intent.putExtra("STATUS", "SUCCESS")
        intent.putExtra("ORDER_ID", orderID)
        intent.putExtra("COURSE_ID", courseId)
        startActivity(intent)
        finish()
    }

    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse?, orderID: String?) {
        Log.e("CheckoutActivity", "Payment failed: ${cfErrorResponse?.message}")
        dismissPaymentLoadingBottomSheet()
        // Navigate to PaymentVerificationActivity
        val intent = Intent(this, PaymentVerificationActivity::class.java)
        intent.putExtra("STATUS", "FAILURE")
        intent.putExtra("ORDER_ID", orderID)
        intent.putExtra("COURSE_ID", courseId)
        startActivity(intent)
    }

    private fun showProfileIncompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Complete Your Profile")
            .setMessage("Please complete your profile before making a purchase. Your profile information is required for enrollment and course access.")
            .setPositiveButton("Complete Profile") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivityForResult(intent, REQUEST_EDIT_PROFILE)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == RESULT_OK) {
            // Reload profile
            loadUserProfile()
        }
    }

    companion object {
        const val REQUEST_EDIT_PROFILE = 100
    }
}
