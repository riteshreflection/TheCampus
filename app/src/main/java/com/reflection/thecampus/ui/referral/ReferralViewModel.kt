package com.reflection.thecampus.ui.referral

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.UserProfile
import com.reflection.thecampus.data.model.ReferralWallet
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ReferralViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val _referralCode = MutableLiveData<String>()
    val referralCode: LiveData<String> = _referralCode

    private val _wallet = MutableLiveData<ReferralWallet>()
    val wallet: LiveData<ReferralWallet> = _wallet

    private val _withdrawalStatus = MutableLiveData<Result<String>>()
    val withdrawalStatus: LiveData<Result<String>> = _withdrawalStatus

    init {
        fetchReferralCode()
        fetchWallet()
    }

    private fun fetchReferralCode() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.child("referral").child("code").get().addOnSuccessListener { snapshot ->
            val code = snapshot.getValue(String::class.java)
            // Only assign if non-null to satisfy lint
            code?.let { _referralCode.postValue(it) }
            
            if (code == null) {
                // Generate if missing - automatically for all users
                userRef.get().addOnSuccessListener { userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)
                    if (userProfile != null) {
                        generateReferralCode(userId, userProfile)
                    }
                }
            }
        }
    }

    private fun generateReferralCode(userId: String, userProfile: UserProfile) {
        val namePart = userProfile.fullName?.take(4)?.uppercase()?.replace(" ", "") ?: "USER"
        val randomPart = (1000..9999).random()
        val newCode = "CAMPUS-$namePart$randomPart"

        // Check uniqueness (simplified: just set it for now, ideally check if exists)
        database.getReference("users").child(userId).child("referral").child("code").setValue(newCode)
            .addOnSuccessListener {
                _referralCode.postValue(newCode)
            }
    }

    private fun fetchWallet() {
        val userId = auth.currentUser?.uid ?: return
        val walletRef = database.getReference("referralWallets").child(userId)

        walletRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wallet = snapshot.getValue(ReferralWallet::class.java)
                // Only assign if non-null to satisfy lint
                wallet?.let { _wallet.postValue(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun requestWithdrawal(amount: Double, upiId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        thread {
            try {
                val url = URL("https://withdrawal.essyritesh.workers.dev/")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val jsonPayload = """
                    {
                        "userId": "$userId",
                        "amount": $amount,
                        "upiId": "$upiId"
                    }
                """.trimIndent()

                conn.outputStream.use { it.write(jsonPayload.toByteArray()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    _withdrawalStatus.postValue(Result.success("Withdrawal requested successfully"))
                } else {
                    _withdrawalStatus.postValue(Result.failure(Exception("Failed: $responseCode")))
                }
            } catch (e: Exception) {
                _withdrawalStatus.postValue(Result.failure(e))
            }
        }
    }
}
