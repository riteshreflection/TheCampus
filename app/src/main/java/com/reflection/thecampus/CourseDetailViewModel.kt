package com.reflection.thecampus

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.reflection.thecampus.data.model.Offer
import com.reflection.thecampus.data.model.Order
import com.reflection.thecampus.data.model.PriceDetails
import com.reflection.thecampus.data.model.TaxDetails
import com.reflection.thecampus.data.model.TaxSetting
import com.reflection.thecampus.utils.FirebaseConstants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CourseDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = CourseRepository(application.applicationContext)

    private val _course = MutableLiveData<Course>()
    val course: LiveData<Course> = _course

    private val _isEnrolled = MutableLiveData<Boolean>()
    val isEnrolled: LiveData<Boolean> = _isEnrolled

    private val _enrollmentSuccess = MutableLiveData<Boolean>()
    val enrollmentSuccess: LiveData<Boolean> = _enrollmentSuccess

    private val _offers = MutableLiveData<List<Offer>>()
    val offers: LiveData<List<Offer>> = _offers

    private val _priceDetails = MutableLiveData<PriceDetails>()
    val priceDetails: LiveData<PriceDetails> = _priceDetails

    private val _couponValidationMessage = MutableLiveData<String?>()
    val couponValidationMessage: LiveData<String?> = _couponValidationMessage

    private val _orderCreated = MutableLiveData<Order?>()
    val orderCreated: LiveData<Order?> = _orderCreated

    private var currentTaxSettings: List<TaxSetting> = emptyList()

    @SuppressLint("NullSafeMutableLiveData")
    fun loadCourse(courseId: String) {
        timber.log.Timber.d("CourseDetailViewModel.loadCourse() called with courseId: $courseId")

        viewModelScope.launch {
            timber.log.Timber.d("Fetching course from repository...")
            val fetchedCourse = repository.getCourse(courseId)

            if (fetchedCourse != null) {
                timber.log.Timber.d("✓ Course loaded successfully: ${fetchedCourse.basicInfo.name}")
                timber.log.Timber.d("  - Price: ${fetchedCourse.pricing.price}")
                timber.log.Timber.d("  - Thumbnail: ${fetchedCourse.pricing.thumbnailUrl}")
                timber.log.Timber.d("  - Instructors: ${fetchedCourse.instructorIds.size}")
                timber.log.Timber.d("  - Tests: ${fetchedCourse.linkedTests.size}")
                timber.log.Timber.d("  - Classes: ${fetchedCourse.linkedClasses.size}")

                _course.value = fetchedCourse
                
                // Load taxes first, then calculate price
                loadTaxSettings()
                calculateInitialPrice(fetchedCourse)
                
                checkEnrollmentStatus(courseId)
                loadOffers()
            } else {
                timber.log.Timber.w("⚠ Course is null! Failed to load course $courseId")
                _course.value = null
            }
        }
    }

    private fun checkEnrollmentStatus(courseId: String) {
        val currentUser = auth.currentUser
        timber.log.Timber.d("Checking enrollment status for course $courseId, user: ${currentUser?.uid ?: "null"}")

        if (currentUser == null) {
            timber.log.Timber.d("No authenticated user, enrollment status: false")
            _isEnrolled.value = false
            return
        }

        viewModelScope.launch {
            val enrolled = repository.checkEnrollmentStatus(currentUser.uid, courseId)
            timber.log.Timber.d("Enrollment status for course $courseId: $enrolled")
            _isEnrolled.value = enrolled
        }
    }

    fun enrollInCourse(courseId: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            val success = repository.enrollInCourse(currentUser.uid, courseId)
            if (success) {
                _enrollmentSuccess.value = true
                _isEnrolled.value = true
            } else {
                _enrollmentSuccess.value = false
            }
        }
    }

    private suspend fun loadTaxSettings() {
        currentTaxSettings = repository.getTaxSettings()
    }

    private suspend fun loadOffers() {
        _offers.value = repository.getOffers()
    }

    private fun calculateInitialPrice(course: Course) {
        val originalPrice = course.pricing.price.coerceAtLeast(0.0) // Ensure non-negative
        val discount = course.pricing.discount.coerceIn(0, 100) // Clamp to 0-100%
        val discountAmount = originalPrice * discount / 100
        val subtotal = (originalPrice - discountAmount).coerceAtLeast(0.0)
        
        // Calculate Tax based on dynamic settings
        var taxAmount = 0.0
        var taxName = "Tax"
        var taxRate = 0.0
        
        // Find applicable tax
        val applicableTax = currentTaxSettings.find { tax ->
            tax.appliedCourses.contains(course.id)
        }
        
        if (applicableTax != null) {
            taxName = applicableTax.name
            if (applicableTax.type == "percentage") {
                taxRate = applicableTax.value.coerceIn(0.0, 100.0) // Validate tax rate
                taxAmount = subtotal * taxRate / 100
            } else if (applicableTax.type == "flat") {
                taxAmount = applicableTax.value.coerceAtLeast(0.0) // Ensure non-negative
            }
        }
        
        val finalPrice = (subtotal + taxAmount).coerceAtLeast(0.0) // Ensure non-negative

        _priceDetails.value = PriceDetails(
            originalPrice = originalPrice,
            siteDiscount = discountAmount,
            subtotal = subtotal,
            taxDetails = TaxDetails(
                taxName = taxName,
                taxAmount = taxAmount,
                taxRate = taxRate
            ),
            finalPrice = finalPrice,
            amountPaid = finalPrice
        )
    }

    fun applyCoupon(couponCode: String) {
        val course = _course.value ?: return
        val offers = _offers.value ?: return

        val originalPrice = course.pricing.price
        val discount = course.pricing.discount
        val siteDiscountAmount = originalPrice * discount / 100
        val subtotal = originalPrice - siteDiscountAmount

        val coupon = offers.find { it.couponCode.equals(couponCode, ignoreCase = true) }

        if (coupon == null) {
            _couponValidationMessage.value = "Invalid coupon code"
            return
        }

        // Validate Coupon
        if (coupon.status != "active") {
            _couponValidationMessage.value = "Coupon is not active"
            return
        }

        // Check expiry
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val validTillDate = sdf.parse(coupon.validTill)
            if (validTillDate != null && Date().after(validTillDate)) {
                _couponValidationMessage.value = "Coupon has expired"
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check min price
        if (subtotal < coupon.minPrice) {
            _couponValidationMessage.value = "Minimum purchase amount of ₹${coupon.minPrice} required"
            return
        }

        // Check applicable courses
        if (coupon.appliedCourses.isNotEmpty() && !coupon.appliedCourses.contains(course.id)) {
            _couponValidationMessage.value = "Coupon not applicable for this course"
            return
        }

        // Check usage limit
        if (coupon.maxUsage > 0 && coupon.usageCount >= coupon.maxUsage) {
            _couponValidationMessage.value = "Coupon usage limit reached"
            return
        }

        // Apply Discount
        var couponDiscount = 0.0
        if (coupon.discountType == "percentage") {
            val discountValue = coupon.discountValue.coerceIn(0.0, 100.0) // Validate percentage
            couponDiscount = subtotal * discountValue / 100
        } else if (coupon.discountType == "flat") {
            couponDiscount = coupon.discountValue.coerceAtLeast(0.0) // Ensure non-negative
        }

        // Ensure discount doesn't exceed subtotal
        couponDiscount = couponDiscount.coerceAtMost(subtotal)

        val taxableAmount = (subtotal - couponDiscount).coerceAtLeast(0.0)
        
        // Recalculate Tax
        var taxAmount = 0.0
        var taxName = "Tax"
        var taxRate = 0.0
        
        val applicableTax = currentTaxSettings.find { tax ->
            tax.appliedCourses.contains(course.id)
        }
        
        if (applicableTax != null) {
            taxName = applicableTax.name
            if (applicableTax.type == "percentage") {
                taxRate = applicableTax.value
                taxAmount = taxableAmount * taxRate / 100
            } else if (applicableTax.type == "flat") {
                taxAmount = applicableTax.value
            }
        }

        val finalPrice = taxableAmount + taxAmount

        _priceDetails.value = PriceDetails(
            originalPrice = originalPrice,
            siteDiscount = siteDiscountAmount,
            subtotal = subtotal, // This is price after site discount, before coupon
            couponCode = coupon.couponCode,
            couponId = coupon.id,
            couponDiscount = couponDiscount,
            taxDetails = TaxDetails(
                taxName = taxName,
                taxAmount = taxAmount,
                taxRate = taxRate
            ),
            finalPrice = finalPrice,
            amountPaid = finalPrice
        )
        _couponValidationMessage.value = null // Success
    }

    fun removeCoupon() {
        val course = _course.value ?: return
        calculateInitialPrice(course)
    }

    fun createOrder(paymentMethod: String) {
        val currentUser = auth.currentUser ?: return
        val course = _course.value ?: return
        val priceDetails = _priceDetails.value ?: return
        
        // Get user profile for name/email if available, or use Auth details
        val userName = currentUser.displayName ?: "User"
        val userEmail = currentUser.email ?: ""

        // Generate order ID with timestamp: order_<timestamp>
        val timestamp = System.currentTimeMillis()
        val orderId = "order_$timestamp"
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val createdAt = sdf.format(Date())

        val order = Order(
            orderId = orderId,
            userId = currentUser.uid,
            userName = userName,
            userEmail = userEmail,
            courseId = course.id,
            courseName = course.basicInfo.name,
            createdAt = createdAt,
            status = "PENDING",
            paymentMethod = paymentMethod,
            priceDetails = priceDetails
        )

        // Store in /payments/<orderId> instead of /orders
        database.getReference("payments")
            .child(orderId)
            .setValue(order)
            .addOnSuccessListener {
                _orderCreated.value = order
            }
            .addOnFailureListener {
                _orderCreated.value = null
            }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        database.getReference(FirebaseConstants.ORDERS).child(orderId).child("status").setValue(status)
        
        if (status == "SUCCESS") {
            // Increment coupon usage if applied
            val priceDetails = _priceDetails.value
            if (priceDetails?.couponId != null) {
                incrementCouponUsage(priceDetails.couponId)
            }
        }
    }

    private fun incrementCouponUsage(couponId: String) {
        val couponRef = database.getReference(FirebaseConstants.OFFERS).child(couponId).child("usageCount")
        couponRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var currentValue = mutableData.getValue(Int::class.java)
                if (currentValue == null) {
                    currentValue = 0
                }
                mutableData.value = currentValue + 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                // Transaction completed
            }
        })
    }
}
