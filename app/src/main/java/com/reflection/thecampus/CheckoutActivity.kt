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

    private lateinit var cbPaymentPolicy: CheckBox
    private lateinit var cbRefundPolicy: CheckBox
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
        setContentView(R.layout.activity_checkout)

        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        } catch (e: CFException) {
            e.printStackTrace()
        }

        // Set status bar color
        window.statusBarColor = getColor(android.R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

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
        cbPaymentPolicy = findViewById(R.id.cbPaymentPolicy)
        cbRefundPolicy = findViewById(R.id.cbRefundPolicy)
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment)
        
        tilCoupon = findViewById(R.id.tilCoupon)
        etCoupon = findViewById(R.id.etCoupon)
        btnApplyCoupon = findViewById(R.id.btnApplyCoupon)
        layoutAppliedCoupon = findViewById(R.id.layoutAppliedCoupon)
        tvAppliedCouponCode = findViewById(R.id.tvAppliedCouponCode)
        tvAppliedCouponDesc = findViewById(R.id.tvAppliedCouponDesc)
        ivRemoveCoupon = findViewById(R.id.ivRemoveCoupon)

        // Setup Policy Links
        setupPolicyLinks()

        // Enable button only when both checkboxes are checked
        cbPaymentPolicy.setOnCheckedChangeListener { _, _ -> updateButtonState() }
        cbRefundPolicy.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        btnProceedToPayment.setOnClickListener {
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
        btnProceedToPayment.isEnabled = cbPaymentPolicy.isChecked && cbRefundPolicy.isChecked
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("userProfiles")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                userProfile = snapshot.getValue(UserProfile::class.java)
            }
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
        findViewById<TextView>(R.id.tvOriginalPrice).text = "₹${priceDetails.originalPrice.toInt()}"
        findViewById<TextView>(R.id.tvTotalPrice).text = "₹${priceDetails.finalPrice.toInt()}"

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
        
        // Coupon UI
        if (priceDetails.couponCode != null) {
            layoutAppliedCoupon.visibility = View.VISIBLE
            tilCoupon.visibility = View.GONE
            btnApplyCoupon.visibility = View.GONE
            
            tvAppliedCouponCode.text = priceDetails.couponCode
            tvAppliedCouponDesc.text = "₹${priceDetails.couponDiscount.toInt()} saved"
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
        val cbPaymentPolicy = findViewById<CheckBox>(R.id.cbPaymentPolicy)
        val cbRefundPolicy = findViewById<CheckBox>(R.id.cbRefundPolicy)

        setPolicyText(
            cbPaymentPolicy,
            "I accept the Payment Policy",
            "Payment Policy",
            "https://www.thecampus.in/terms-and-conditions"
        )

        setPolicyText(
            cbRefundPolicy,
            "I accept the Refund Policy",
            "Refund Policy",
            "https://www.thecampus.in/refund-and-cancellation-policy"
        )
    }

    private fun setPolicyText(checkBox: CheckBox, fullText: String, clickablePart: String, url: String) {
        val spannableString = SpannableString(fullText)
        val startIndex = fullText.indexOf(clickablePart)
        val endIndex = startIndex + clickablePart.length

        if (startIndex >= 0) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Prevent checkbox toggling when clicking the link
                    // Note: This might be tricky as the checkbox consumes clicks. 
                    // Usually setting movementMethod is enough for TextView, but CheckBox might toggle.
                    // Let's open the URL.
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = resources.getColor(R.color.colorPrimary, theme)
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        checkBox.text = spannableString
        checkBox.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onPaymentVerify(orderID: String?) {
        Log.d("CheckoutActivity", "Payment verified for order: $orderID")
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
