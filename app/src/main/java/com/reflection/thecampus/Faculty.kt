package com.reflection.thecampus

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@IgnoreExtraProperties
data class Faculty(
    val id: String = "",
    val name: String = "",
    val profilePictureUrl: String = "",
    val specifications: String = "",
    val academicExcellence: String = "",
    val experience: String = "",
    val email: String = "",
    val bio: String = ""
) : Parcelable
