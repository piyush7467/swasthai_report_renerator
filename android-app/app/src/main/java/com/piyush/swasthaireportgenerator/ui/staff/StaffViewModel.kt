package com.piyush.swasthaireportgenerator.ui.staff

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.model.CreateLabStaffRequest
import com.piyush.swasthaireportgenerator.data.model.LabStaffDetailsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse
import com.piyush.swasthaireportgenerator.data.repository.LabStaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StaffUiState(
    val summary: LabStaffSummaryResponse? = null,
    val staffList: List<LabStaffResponse> = emptyList(),
    val selectedStatusFilter: String? = null, // null = ALL, ACTIVE, INACTIVE
    val searchQuery: String = "",
    val selectedDetails: LabStaffDetailsResponse? = null,
    val isLoading: Boolean = false,
    val isDetailsLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val createError: String? = null
)

class StaffViewModel(application: Application) : AndroidViewModel(application) {

    private val staffRepository = LabStaffRepository(ApiClient.getLabStaffApi(application))

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = StaffUiState()
    }

    fun refreshAll() {
        loadSummary()
        loadStaffList(_uiState.value.selectedStatusFilter, _uiState.value.searchQuery)
    }

    fun loadSummary() {
        viewModelScope.launch {
            val result = staffRepository.getSummary()
            result.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(summary = summary)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err.localizedMessage ?: "Failed to load staff summary"
                )
            }
        }
    }

    fun loadStaffList(status: String? = _uiState.value.selectedStatusFilter, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedStatusFilter = status,
                searchQuery = search,
                errorMessage = null
            )
            val result = staffRepository.getStaffList(status = status, search = search)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    staffList = list
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load lab staff"
                )
            }
        }
    }

    fun clearCreateError() {
        _uiState.value = _uiState.value.copy(createError = null)
    }

    fun createStaff(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, createError = null)
            val result = staffRepository.createStaff(
                CreateLabStaffRequest(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isMutating = false, createError = null)
                refreshAll()
                onSuccess()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isMutating = false,
                    createError = err.localizedMessage ?: "Failed to create staff account"
                )
            }
        }
    }

    fun loadStaffDetails(refId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailsLoading = true, selectedDetails = null)
            val result = staffRepository.getStaffDetails(refId)
            result.onSuccess { details ->
                _uiState.value = _uiState.value.copy(
                    isDetailsLoading = false,
                    selectedDetails = details
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isDetailsLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load staff details"
                )
            }
        }
    }

    fun clearSelectedDetails() {
        _uiState.value = _uiState.value.copy(selectedDetails = null)
    }

    fun toggleStaffStatus(staff: LabStaffResponse) {
        val nextStatus = if (staff.status.equals("ACTIVE", ignoreCase = true)) "INACTIVE" else "ACTIVE"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, errorMessage = null)
            val result = staffRepository.updateStaffStatus(staff.refId, nextStatus)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isMutating = false)
                refreshAll()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isMutating = false,
                    errorMessage = err.localizedMessage ?: "Failed to update staff status"
                )
            }
        }
    }

    fun deleteStaff(refId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, errorMessage = null)
            val result = staffRepository.deleteStaff(refId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isMutating = false)
                refreshAll()
                onSuccess()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isMutating = false,
                    errorMessage = err.localizedMessage ?: "Failed to delete staff member"
                )
            }
        }
    }
}
