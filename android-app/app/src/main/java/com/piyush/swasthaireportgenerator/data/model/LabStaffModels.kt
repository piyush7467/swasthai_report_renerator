package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class LabStaffSummaryResponse(
    @SerializedName("totalStaff") val totalStaff: Long = 0,
    @SerializedName("activeStaff") val activeStaff: Long = 0,
    @SerializedName("maxLabStaff") val maxLabStaff: Int = 0,
    @SerializedName("remainingSlots") val remainingSlots: Int = 0,
    @SerializedName("limitReached") val limitReached: Boolean = false,
    @SerializedName("planCode") val planCode: String? = null,
    @SerializedName("planName") val planName: String? = null
)

data class LabStaffResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("lastLoginAt") val lastLoginAt: String? = null,
    @SerializedName("inactiveAt") val inactiveAt: String? = null,
    @SerializedName("eligibleForCleanupAt") val eligibleForCleanupAt: String? = null,
    @SerializedName("cleanupEligible") val cleanupEligible: Boolean? = null,
    @SerializedName("deactivatedByName") val deactivatedByName: String? = null,
    @SerializedName("reportsCreated") val reportsCreated: Long? = null,
    @SerializedName("reportsFinalized") val reportsFinalized: Long? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class LabStaffDetailsResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("organizationName") val organizationName: String? = null,
    @SerializedName("lastLoginAt") val lastLoginAt: String? = null,
    @SerializedName("inactiveAt") val inactiveAt: String? = null,
    @SerializedName("eligibleForCleanupAt") val eligibleForCleanupAt: String? = null,
    @SerializedName("cleanupEligible") val cleanupEligible: Boolean? = null,
    @SerializedName("deactivatedByName") val deactivatedByName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("reportsCreated") val reportsCreated: Long = 0,
    @SerializedName("reportsFinalized") val reportsFinalized: Long = 0
)

data class CreateLabStaffRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirmPassword") val confirmPassword: String
)

data class UpdateUserStatusRequest(
    @SerializedName("status") val status: String
)
