package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class LicenseResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("planRefId") val planRefId: String? = null,
    @SerializedName("planCode") val planCode: String? = null,
    @SerializedName("planName") val planName: String? = null,
    @SerializedName("maxLabStaff") val maxLabStaff: Int? = null,
    @SerializedName("maxReportsPerMonth") val maxReportsPerMonth: Int? = null,
    @SerializedName("maxReportsPerDay") val maxReportsPerDay: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("currentlyUsable") val currentlyUsable: Boolean = false
)

data class OrganizationLicenseOverviewResponse(
    @SerializedName("license") val license: LicenseResponse? = null,
    @SerializedName("staffUsage") val staffUsage: StaffUsageSummary? = null,
    @SerializedName("patientUsage") val patientUsage: PatientUsageSummary? = null,
    @SerializedName("reportUsage") val reportUsage: ReportUsageSummary? = null,
    @SerializedName("daysRemaining") val daysRemaining: Long = 0,
    @SerializedName("expiryStatus") val expiryStatus: String? = null
) {
    data class StaffUsageSummary(
        @SerializedName("activeStaff") val activeStaff: Long = 0,
        @SerializedName("maxLabStaff") val maxLabStaff: Int = 0,
        @SerializedName("remainingSlots") val remainingSlots: Int = 0,
        @SerializedName("totalStaff") val totalStaff: Long = 0,
        @SerializedName("limitReached") val limitReached: Boolean = false,
        @SerializedName("overLimit") val overLimit: Boolean = false
    )

    data class PatientUsageSummary(
        @SerializedName("totalPatients") val totalPatients: Long = 0
    )

    data class ReportUsageSummary(
        @SerializedName("totalReports") val totalReports: Long = 0,
        @SerializedName("finalizedReports") val finalizedReports: Long = 0,
        @SerializedName("monthlyReportsCreated") val monthlyReportsCreated: Long = 0,
        @SerializedName("maxReportsPerMonth") val maxReportsPerMonth: Int = 0,
        @SerializedName("dailyReportsCreated") val dailyReportsCreated: Long = 0,
        @SerializedName("maxReportsPerDay") val maxReportsPerDay: Int = 0,
        @SerializedName("remainingMonthlyReports") val remainingMonthlyReports: Int = 0,
        @SerializedName("monthlyLimitReached") val monthlyLimitReached: Boolean = false,
        @SerializedName("dailyLimitReached") val dailyLimitReached: Boolean = false
    )
}

data class AvailablePlanResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("annualPrice") val annualPrice: Double? = null,
    @SerializedName("currency") val currency: String? = "INR",
    @SerializedName("maxLabStaff") val maxLabStaff: Int = 0,
    @SerializedName("maxReportsPerMonth") val maxReportsPerMonth: Int? = null,
    @SerializedName("maxReportsPerDay") val maxReportsPerDay: Int? = null,
    @SerializedName("currentPlan") val currentPlan: Boolean = false,
    @SerializedName("upgrade") val upgrade: Boolean = false
)

data class PlanUpgradeRequestResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("organizationName") val organizationName: String? = null,
    @SerializedName("currentPlanName") val currentPlanName: String? = null,
    @SerializedName("requestedPlanRefId") val requestedPlanRefId: String? = null,
    @SerializedName("requestedPlanName") val requestedPlanName: String? = null,
    @SerializedName("currentActiveStaffCount") val currentActiveStaffCount: Int = 0,
    @SerializedName("requestedStaffCapacity") val requestedStaffCapacity: Int = 0,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("contactName") val contactName: String? = null,
    @SerializedName("contactEmail") val contactEmail: String? = null,
    @SerializedName("contactPhone") val contactPhone: String? = null,
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("adminNotes") val adminNotes: String? = null,
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreateUpgradeRequest(
    @SerializedName("requestedPlanRefId") val requestedPlanRefId: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("contactName") val contactName: String,
    @SerializedName("contactEmail") val contactEmail: String,
    @SerializedName("contactPhone") val contactPhone: String? = null,
    @SerializedName("additionalMessage") val additionalMessage: String? = null
)
