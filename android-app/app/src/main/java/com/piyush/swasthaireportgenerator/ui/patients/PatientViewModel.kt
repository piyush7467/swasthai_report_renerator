package com.piyush.swasthaireportgenerator.ui.patients

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.CreatePatientRequest
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.UpdatePatientRequest
import com.piyush.swasthaireportgenerator.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PatientListUiState(
    val patients: List<PatientResponse> = emptyList(),
    val userRole: String = "LAB_STAFF",
    val selectedPatient: PatientResponse? = null,
    val selectedPatientReports: List<ReportResponse> = emptyList(),
    val isProfileLoading: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val createError: String? = null,
    val isCreating: Boolean = false
)

class PatientViewModel(application: Application) : AndroidViewModel(application) {

    private val patientRepository = PatientRepository(ApiClient.getPatientApi(application))
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(PatientListUiState())
    val uiState: StateFlow<PatientListUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = PatientListUiState()
    }

    fun loadPatients(search: String? = null) {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            if (session == null || session.accessToken.isBlank()) return@launch
            val role = session.role.ifBlank { "LAB_STAFF" }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                userRole = role
            )
            val result = patientRepository.getPatients(search = search)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    patients = list,
                    searchQuery = search ?: ""
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load patients"
                )
            }
        }
    }

    fun loadPatientProfile(patientRefId: String) {
        viewModelScope.launch {
            val role = tokenManager.userSessionFlow.first()?.role ?: "LAB_STAFF"
            _uiState.value = _uiState.value.copy(
                isProfileLoading = true,
                errorMessage = null,
                userRole = role
            )
            val patientRes = patientRepository.getPatient(patientRefId)
            val reportsRes = patientRepository.getPatientReports(patientRefId)

            _uiState.value = _uiState.value.copy(
                isProfileLoading = false,
                selectedPatient = patientRes.getOrNull(),
                selectedPatientReports = reportsRes.getOrDefault(emptyList()),
                errorMessage = patientRes.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun clearCreateError() {
        _uiState.value = _uiState.value.copy(createError = null)
    }

    fun createPatient(
        salutation: String,
        name: String,
        age: Int,
        ageUnit: String,
        gender: String,
        phone: String?,
        email: String?,
        address: String?,
        onSuccess: (PatientResponse) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)

            val cleanedPhone = phone
                ?.trim()
                ?.replace(Regex("[\\s-]"), "")
                ?.ifBlank { null }

            val request = CreatePatientRequest(
                salutation = salutation,
                name = name.trim(),
                dateOfBirthKnown = false,
                dateOfBirth = null,
                ageValue = age,
                ageUnit = ageUnit,
                gender = gender,
                phone = cleanedPhone,
                email = email?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null }
            )

            val result = patientRepository.createPatient(request)
            result.onSuccess { patient ->
                val updatedList = listOf(patient) + _uiState.value.patients.filterNot { it.refId == patient.refId }
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = null,
                    patients = updatedList
                )
                onSuccess(patient)
                loadPatients()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = error.localizedMessage ?: "Failed to create patient"
                )
            }
        }
    }

    fun updatePatient(
        patientRefId: String,
        salutation: String,
        name: String,
        age: Int,
        ageUnit: String,
        gender: String,
        phone: String?,
        email: String?,
        address: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)

            val cleanedPhone = phone
                ?.trim()
                ?.replace(Regex("[\\s-]"), "")
                ?.ifBlank { null }

            val request = UpdatePatientRequest(
                salutation = salutation,
                name = name.trim(),
                dateOfBirthKnown = false,
                dateOfBirth = null,
                ageValue = age,
                ageUnit = ageUnit,
                gender = gender,
                phone = cleanedPhone,
                email = email?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null }
            )

            val result = patientRepository.updatePatient(patientRefId, request)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = null,
                    selectedPatient = updated
                )
                loadPatients()
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = error.localizedMessage ?: "Failed to update patient"
                )
            }
        }
    }

    fun deletePatient(patientRefId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
            val result = patientRepository.deletePatient(patientRefId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isCreating = false, selectedPatient = null)
                loadPatients()
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    errorMessage = error.localizedMessage ?: "Failed to delete patient"
                )
            }
        }
    }
}
