package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class OrganizationTestResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("organizationName") val organizationName: String? = null,
    @SerializedName("testRefId") val testRefId: String,
    @SerializedName("testCode") val testCode: String,
    @SerializedName("testName") val testName: String,
    @SerializedName("testType") val testType: String? = null,
    @SerializedName("status") val status: String? = null
)

data class ReportResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("organizationRefId") val organizationRefId: String? = null,
    @SerializedName("organizationName") val organizationName: String? = null,
    @SerializedName("patientRefId") val patientRefId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("reportVersion") val reportVersion: Int? = null,
    @SerializedName("lockVersion") val lockVersion: Long? = null,
    @SerializedName("createdByEmail") val createdByEmail: String? = null,
    @SerializedName("finalizedByEmail") val finalizedByEmail: String? = null,
    @SerializedName("finalizedAt") val finalizedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("tests") val tests: List<ReportTestItemResponse> = emptyList(),
    @SerializedName("includeOrganizationHeader") val includeOrganizationHeader: Boolean? = null,

    // Flat Patient Snapshot Fields returned by backend ReportResponse
    @SerializedName("patientName") val patientName: String? = null,
    @SerializedName("patientSalutation") val patientSalutation: String? = null,
    @SerializedName("patientCode") val patientCode: String? = null,
    @SerializedName("patientGender") val patientGender: String? = null,
    @SerializedName("patientAgeAtReportingValue") val patientAgeAtReportingValue: Int? = null,
    @SerializedName("patientAgeAtReportingUnit") val patientAgeAtReportingUnit: String? = null,
    @SerializedName("patientPhone") val patientPhone: String? = null,

    // Creator & Finalizer Snapshot
    @SerializedName("createdByName") val createdByName: String? = null,
    @SerializedName("finalizedByName") val finalizedByName: String? = null,

    // Organization Snapshot Fields
    @SerializedName("organizationAddressLine1") val organizationAddressLine1: String? = null,
    @SerializedName("organizationAddressLine2") val organizationAddressLine2: String? = null,
    @SerializedName("organizationCity") val organizationCity: String? = null,
    @SerializedName("organizationState") val organizationState: String? = null,
    @SerializedName("organizationPostalCode") val organizationPostalCode: String? = null,
    @SerializedName("organizationCountry") val organizationCountry: String? = null,
    @SerializedName("organizationPhone") val organizationPhone: String? = null,
    @SerializedName("organizationEmail") val organizationEmail: String? = null,
    @SerializedName("organizationWebsite") val organizationWebsite: String? = null,
    @SerializedName("organizationSignatureOwnerName") val organizationSignatureOwnerName: String? = null,
    @SerializedName("organizationReportFooterText") val organizationReportFooterText: String? = null,
    @SerializedName("organizationReportDisclaimer") val organizationReportDisclaimer: String? = null
) {
    val displayPatientName: String
        get() = listOfNotNull(
            patientSalutation?.takeIf { it.isNotBlank() }?.let { "$it." },
            patientName?.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { "Patient (${refId.take(8)})" }

    val displayAgeGender: String
        get() {
            val agePart = if (patientAgeAtReportingValue != null) {
                "$patientAgeAtReportingValue ${patientAgeAtReportingUnit?.lowercase() ?: "yrs"}"
            } else null
            val genderPart = patientGender?.takeIf { it.isNotBlank() }
            return listOfNotNull(agePart, genderPart).joinToString(" • ").ifBlank { "—" }
        }
}

data class ReportTestItemResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("testRefId") val testRefId: String? = null,
    @SerializedName("testCode") val testCode: String,
    @SerializedName("testName") val testName: String,
    @SerializedName("displayOrder") val displayOrder: Int? = null,
    @SerializedName("testVersion") val testVersion: Int? = null,
    @SerializedName("parameters") val parameters: List<ReportParameterResultResponse> = emptyList()
)

data class ReportParameterResultResponse(
    @SerializedName("refId") val refId: String? = null,
    @SerializedName("parameterRefId") val parameterRefId: String,
    @SerializedName("parameterCode") val parameterCode: String,
    @SerializedName("parameterName") val parameterName: String,
    @SerializedName("dataType") val dataType: String? = null,
    @SerializedName("inputType") val inputType: String? = null,
    @SerializedName("calculationType") val calculationType: String? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("referenceMin") val referenceMin: Double? = null,
    @SerializedName("referenceMax") val referenceMax: Double? = null,
    @SerializedName("criticalLow") val criticalLow: Double? = null,
    @SerializedName("criticalHigh") val criticalHigh: Double? = null,
    @SerializedName("flag") val flag: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int? = null
)

data class CreateReportRequest(
    @SerializedName("patientRefId") val patientRefId: String,
    @SerializedName("includeOrganizationHeader") val includeOrganizationHeader: Boolean = true
)

data class AddReportTestsBulkRequest(
    @SerializedName("testRefIds") val testRefIds: List<String>,
    @SerializedName("lockVersion") val lockVersion: Long? = null
)

data class UpdateReportParametersRequest(
    @SerializedName("lockVersion") val lockVersion: Long? = null,
    @SerializedName("parameters") val parameters: List<ParameterValueUpdate>
)

data class ParameterValueUpdate(
    @SerializedName("parameterRefId") val parameterRefId: String,
    @SerializedName("parameterCode") val parameterCode: String,
    @SerializedName("value") val value: String
)

data class UpdateReportHeaderOptionRequest(
    @SerializedName("includeOrganizationHeader") val includeOrganizationHeader: Boolean
)

data class CreateReportShareRequest(
    @SerializedName("channel") val channel: String = "LINK",
    @SerializedName("recipient") val recipient: String? = null,
    @SerializedName("expiresInDays") val expiresInDays: Int = 7
)

data class ReportShareResponse(
    @SerializedName("shareToken") val shareToken: String,
    @SerializedName("reportRefId") val reportRefId: String,
    @SerializedName("shareChannel") val shareChannel: String,
    @SerializedName("shareUrl") val shareUrl: String,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)
