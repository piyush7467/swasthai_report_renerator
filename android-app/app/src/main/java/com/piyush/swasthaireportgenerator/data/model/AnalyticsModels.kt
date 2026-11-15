package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class OrgOverviewStatsResponse(
    @SerializedName("totalReports") val totalReports: Long = 0,
    @SerializedName("finalizedReports") val finalizedReports: Long = 0,
    @SerializedName("draftReports") val draftReports: Long = 0,
    @SerializedName("cancelledReports") val cancelledReports: Long = 0,
    @SerializedName("pendingReviewReports") val pendingReviewReports: Long = 0,
    @SerializedName("totalPatients") val totalPatients: Long = 0,
    @SerializedName("activeTests") val activeTests: Long = 0,
    @SerializedName("reportsToday") val reportsToday: Long = 0
)

data class OrgActivityItemResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("description") val description: String = "",
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CategoryUsageStatsResponse(
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("count") val count: Long = 0
)

data class OrganizationProfileResponse(
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("organizationName") val organizationName: String? = null,
    @SerializedName("addressLine1") val addressLine1: String? = null,
    @SerializedName("addressLine2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postalCode") val postalCode: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("alternatePhone") val alternatePhone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("logoConfigured") val logoConfigured: Boolean = false,
    @SerializedName("signatureConfigured") val signatureConfigured: Boolean = false,
    @SerializedName("signatureOwnerName") val signatureOwnerName: String? = null,
    @SerializedName("signatureOwnerEmail") val signatureOwnerEmail: String? = null,
    @SerializedName("signatureVerificationStatus") val signatureVerificationStatus: String? = null,
    @SerializedName("signatureRejectionReason") val signatureRejectionReason: String? = null,
    @SerializedName("reportFooterText") val reportFooterText: String? = null,
    @SerializedName("reportDisclaimer") val reportDisclaimer: String? = null
) {
    val displayAddress: String
        get() = listOfNotNull(addressLine1, addressLine2, city, state, postalCode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

data class UpdateOrganizationProfileRequest(
    @SerializedName("addressLine1") val addressLine1: String? = null,
    @SerializedName("addressLine2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postalCode") val postalCode: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("alternatePhone") val alternatePhone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("reportFooterText") val reportFooterText: String? = null,
    @SerializedName("reportDisclaimer") val reportDisclaimer: String? = null
)

data class TestParameterResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("testRefId") val testRefId: String? = null,
    @SerializedName("testCode") val testCode: String? = null,
    @SerializedName("testName") val testName: String? = null,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("dataType") val dataType: String? = null,
    @SerializedName("inputType") val inputType: String? = null,
    @SerializedName("calculationType") val calculationType: String? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("required") val required: Boolean = false,
    @SerializedName("displayOrder") val displayOrder: Int? = null,
    @SerializedName("referenceMin") val referenceMin: Double? = null,
    @SerializedName("referenceMax") val referenceMax: Double? = null,
    @SerializedName("criticalLow") val criticalLow: Double? = null,
    @SerializedName("criticalHigh") val criticalHigh: Double? = null,
    @SerializedName("status") val status: String? = null
)
