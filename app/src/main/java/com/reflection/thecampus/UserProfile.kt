package com.reflection.thecampus

import android.os.Parcelable
import androidx.annotation.Keep
import com.reflection.thecampus.utils.InputValidator
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val profilePictureUrl: String = "",
    val createdAt: Long = 0
) : Parcelable {
    
    // Check if profile is complete with proper validation
    fun isComplete(): Boolean {
        return fullName.isNotBlank() && 
               mobileNumber.isNotBlank() && 
               email.isNotBlank() &&
               InputValidator.isValidName(fullName) &&
               InputValidator.isValidPhone(mobileNumber) &&
               InputValidator.isValidEmail(email)
    }
    
    // Get detailed validation errors
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        InputValidator.getNameErrorMessage(fullName)?.let { errors.add(it) }
        InputValidator.getPhoneErrorMessage(mobileNumber)?.let { errors.add(it) }
        InputValidator.getEmailErrorMessage(email)?.let { errors.add(it) }
        
        return errors
    }
}
