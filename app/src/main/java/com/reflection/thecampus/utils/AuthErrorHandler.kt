package com.reflection.thecampus.utils

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

/**
 * Utility object for converting technical Firebase errors into user-friendly messages
 */
object AuthErrorHandler {
    
    /**
     * Convert Firebase Auth exception to user-friendly message
     */
    fun getErrorMessage(exception: Exception?): String {
        if (exception == null) return "Something went wrong. Please try again."
        
        // Handle network errors first
        if (exception is FirebaseNetworkException) {
            return "No internet connection. Please check your network and try again."
        }
        
        // Handle Firebase Auth specific errors
        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                // Login errors
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                "ERROR_USER_NOT_FOUND" -> "No account found with this email. Please sign up first."
                "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support."
                "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password. Please check and try again."
                
                // Signup errors
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Please login instead."
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use at least 6 characters."
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                
                // Rate limiting
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a few minutes and try again."
                
                // Account state
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> 
                    "An account already exists with this email but different sign-in method. Try signing in with email/password or Google."
                
                // Default
                else -> "Authentication failed. Please try again."
            }
        }
        
        // Parse error message for common patterns
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            message.contains("network") || message.contains("connection") ->
                "Network error. Please check your internet connection."
            
            message.contains("invalid-credential") || message.contains("invalid credential") ->
                "Invalid email or password. Please check and try again."
            
            message.contains("user-not-found") || message.contains("no user") ->
                "No account found with this email. Please sign up first."
            
            message.contains("wrong-password") || message.contains("incorrect password") ->
                "Incorrect password. Please try again."
            
            message.contains("email-already-in-use") || message.contains("already exists") ->
                "An account with this email already exists. Please login instead."
            
            message.contains("weak-password") ->
                "Password is too weak. Please use at least 6 characters."
            
            message.contains("invalid-email") || message.contains("badly formatted") ->
                "Please enter a valid email address."
            
            message.contains("too-many-requests") ->
                "Too many attempts. Please wait a few minutes and try again."
            
            message.contains("user-disabled") ->
                "This account has been disabled. Please contact support."
            
            message.contains("requires-recent-login") ->
                "For security, please login again to continue."
            
            else -> "Something went wrong. Please try again."
        }
    }
    
    /**
     * Get helpful suggestion based on error type
     */
    fun getErrorSuggestion(exception: Exception?): String? {
        if (exception == null) return null
        
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            message.contains("network") || message.contains("connection") ->
                "Make sure you're connected to the internet"
            
            message.contains("wrong-password") || message.contains("incorrect password") ->
                "Forgot your password? Tap 'Forgot Password' below"
            
            message.contains("user-not-found") ->
                "Don't have an account? Sign up to get started"
            
            message.contains("email-already-in-use") ->
                "Already have an account? Login instead"
            
            message.contains("weak-password") ->
                "Try using a mix of letters, numbers, and symbols"
            
            else -> null
        }
    }
}
