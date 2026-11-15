package com.piyush.swasthaireportgenerator.ui.license

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.AvailablePlanResponse
import com.piyush.swasthaireportgenerator.data.model.CreateUpgradeRequest
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationLicenseOverviewResponse
import com.piyush.swasthaireportgenerator.data.model.PlanUpgradeRequestResponse
import com.piyush.swasthaireportgenerator.data.repository.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LicenseUiState(
    val userRole: String = "LAB_STAFF",
    val userName: String = "",
    val userEmail: String = "",
    val overview: OrganizationLicenseOverviewResponse? = null,
    val basicLicense: LicenseResponse? = null,
    val availablePlans: List<AvailablePlanResponse> = emptyList(),
    val upgradeRequests: List<PlanUpgradeRequestResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitError: String? = null
)

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val licenseRepository = LicenseRepository(ApiClient.getLicenseApi(application))
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = LicenseUiState()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            val role = session?.role ?: "LAB_STAFF"

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                userRole = role,
                userName = session?.name ?: "",
                userEmail = session?.email ?: ""
            )

            if (role.equals("ORG_ADMIN", ignoreCase = true)) {
                val overviewResult = licenseRepository.getOverview()
                val plansResult = licenseRepository.getAvailablePlans()
                val requestsResult = licenseRepository.getMyUpgradeRequests()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    overview = overviewResult.getOrNull(),
                    basicLicense = overviewResult.getOrNull()?.license,
                    availablePlans = plansResult.getOrDefault(emptyList()),
                    upgradeRequests = requestsResult.getOrDefault(emptyList()),
                    errorMessage = overviewResult.exceptionOrNull()?.localizedMessage
                )
            } else {
                val licenseResult = licenseRepository.getOwnLicense()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    basicLicense = licenseResult.getOrNull(),
                    errorMessage = licenseResult.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun clearSubmitError() {
        _uiState.value = _uiState.value.copy(submitError = null)
    }

    fun submitUpgradeRequest(
        requestedPlanRefId: String,
        contactName: String,
        contactEmail: String,
        contactPhone: String?,
        reason: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val result = licenseRepository.createUpgradeRequest(
                CreateUpgradeRequest(
                    requestedPlanRefId = requestedPlanRefId,
                    contactName = contactName.trim(),
                    contactEmail = contactEmail.trim(),
                    contactPhone = contactPhone?.trim()?.ifBlank { null },
                    reason = reason?.trim()?.ifBlank { null }
                )
            )
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = null)
                refreshAll()
                onSuccess()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = err.localizedMessage ?: "Failed to submit upgrade request"
                )
            }
        }
    }

    fun cancelRequest(refId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val result = licenseRepository.cancelUpgradeRequest(refId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                refreshAll()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = err.localizedMessage ?: "Failed to cancel request"
                )
            }
        }
    }
}
