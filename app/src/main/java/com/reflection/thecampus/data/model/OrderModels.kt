package com.reflection.thecampus.data.model

import com.google.firebase.database.PropertyName
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val createdAt: String = "", // ISO 8601
    val status: String = "", // PENDING, SUCCESS, FAILED
    val paymentMethod: String = "",
    val priceDetails: PriceDetails = PriceDetails(),
    val isUpgrade: Boolean = false,
    val upgradeInfo: String? = null // Placeholder for now
) : Parcelable

@Keep
@Parcelize
data class PriceDetails(
    val originalPrice: Double = 0.0,
    val siteDiscount: Double = 0.0,
    val couponCode: String? = null,
    val couponId: String? = null,
    val couponDiscount: Double = 0.0,
    val subtotal: Double = 0.0,
    val taxDetails: TaxDetails? = null,
    val finalPrice: Double = 0.0,
    val amountPaid: Double = 0.0
) : Parcelable

@Keep
@Parcelize
data class TaxDetails(
    val taxName: String = "GST",
    val taxRate: Double = 18.0,
    val taxType: String = "percentage",
    val taxAmount: Double = 0.0
) : Parcelable

@Keep
@Parcelize
data class Offer(
    val id: String = "",
    val title: String = "",
    val couponCode: String = "",
    val discountType: String = "", // percentage, flat
    val discountValue: Double = 0.0,
    val minPrice: Double = 0.0,
    val validTill: String = "", // ISO 8601
    val appliedCourses: List<String> = emptyList(),
    val status: String = "", // active, expired, disabled
    @get:PropertyName("isPublic")
    val isPublic: Boolean = false,
    val maxUsage: Int = 0,
    val usageCount: Int = 0,
    val createdAt: Long = 0,
    val createdBy: String = ""
) : Parcelable

@Keep
@Parcelize
data class TaxSetting(
    val id: String = "",
    val name: String = "",
    val type: String = "", // percentage, flat
    val value: Double = 0.0,
    @get:PropertyName("isAvoidable")
    val isAvoidable: Boolean = false,
    val appliedCourses: List<String> = emptyList()
) : Parcelable
