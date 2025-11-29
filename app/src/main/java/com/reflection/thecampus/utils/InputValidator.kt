package com.reflection.thecampus.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Utility for input validation and sanitization
 */
object InputValidator {
    
    // Regex patterns
    private val PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$") // Indian phone number
    private val NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s.'-]{2,50}$")
    private val COUPON_PATTERN = Pattern.compile("^[A-Z0-9]{4,20}$")
    
    /**
     * Validate Indian mobile number (10 digits, starting with 6-9)
     */
    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.trim()
        return PHONE_PATTERN.matcher(cleanPhone).matches()
    }
    
    /**
     * Validate email address
     */
    fun isValidEmail(email: String): Boolean {
        val cleanEmail = email.trim()
        return cleanEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
    }
    
    /**
     * Validate name (2-50 characters, letters, spaces, and common name punctuation)
     */
    fun isValidName(name: String): Boolean {
        val cleanName = name.trim()
        return cleanName.isNotEmpty() && NAME_PATTERN.matcher(cleanName).matches()
    }
    
    /**
     * Validate coupon code (4-20 uppercase alphanumeric)
     */
    fun isValidCouponCode(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        return COUPON_PATTERN.matcher(cleanCode).matches()
    }
    
    /**
     * Sanitize chat message to prevent XSS
     * Removes HTML tags and escapes special characters
     */
    fun sanitizeChatMessage(message: String): String {
        if (message.isBlank()) return ""
        
        var sanitized = message.trim()
        
        // Remove HTML tags
        sanitized = sanitized.replace(Regex("<[^>]*>"), "")
        
        // Escape special characters
        sanitized = sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
        
        // Limit length
        return if (sanitized.length > 1000) {
            sanitized.substring(0, 1000)
        } else {
            sanitized
        }
    }
    
    /**
     * Validate UPI ID format
     */
    fun isValidUpiId(upiId: String): Boolean {
        val cleanUpi = upiId.trim()
        // Format: username@bankname
        val upiPattern = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")
        return upiPattern.matcher(cleanUpi).matches()
    }
    
    /**
     * Validate amount (positive number, max 2 decimal places)
     */
    fun isValidAmount(amount: String): Boolean {
        if (amount.isBlank()) return false
        
        return try {
            val value = amount.toDoubleOrNull() ?: return false
            value > 0 && value <= 1000000 // Max 10 lakh
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get validation error message for phone
     */
    fun getPhoneErrorMessage(phone: String): String? {
        return when {
            phone.isBlank() -> "Phone number is required"
            phone.length != 10 -> "Phone number must be 10 digits"
            !phone[0].toString().matches(Regex("[6-9]")) -> "Phone number must start with 6, 7, 8, or 9"
            !isValidPhone(phone) -> "Invalid phone number format"
            else -> null
        }
    }
    
    /**
     * Get validation error message for email
     */
    fun getEmailErrorMessage(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !isValidEmail(email) -> "Invalid email format"
            else -> null
        }
    }
    
    /**
     * Get validation error message for name
     */
    fun getNameErrorMessage(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must not exceed 50 characters"
            !isValidName(name) -> "Name can only contain letters, spaces, and common punctuation"
            else -> null
        }
    }
}

/**
 * Extension functions for easy validation
 */
fun String.isValidPhone() = InputValidator.isValidPhone(this)
fun String.isValidEmail() = InputValidator.isValidEmail(this)
fun String.isValidName() = InputValidator.isValidName(this)
fun String.sanitizeForChat() = InputValidator.sanitizeChatMessage(this)
