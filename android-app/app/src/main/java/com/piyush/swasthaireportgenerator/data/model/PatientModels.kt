package com.piyush.swasthaireportgenerator.data.model

import com.google.gson.annotations.SerializedName

data class PatientResponse(
    @SerializedName("refId") val refId: String,
    @SerializedName("patientCode") val patientCode: String,
    @SerializedName("salutation") val salutation: String?,
    @SerializedName("name") val name: String,
    @SerializedName("dateOfBirthKnown") val dateOfBirthKnown: Boolean = false,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("ageValue") val ageValue: Int?,
    @SerializedName("ageUnit") val ageUnit: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("totalReports") val totalReports: Long? = null,
    @SerializedName("lastReportDate") val lastReportDate: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreatePatientRequest(
    @SerializedName("salutation") val salutation: String = "MR",
    @SerializedName("name") val name: String,
    @SerializedName("dateOfBirthKnown") val dateOfBirthKnown: Boolean = false,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("ageValue") val ageValue: Int? = null,
    @SerializedName("ageUnit") val ageUnit: String? = "YEARS",
    @SerializedName("gender") val gender: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("weightKg") val weightKg: Double? = null
)

typealias UpdatePatientRequest = CreatePatientRequest
