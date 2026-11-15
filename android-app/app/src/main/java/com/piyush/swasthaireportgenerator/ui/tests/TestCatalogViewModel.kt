package com.piyush.swasthaireportgenerator.ui.tests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.model.OrganizationTestResponse
import com.piyush.swasthaireportgenerator.data.model.TestParameterResponse
import com.piyush.swasthaireportgenerator.data.repository.DashboardRepository
import com.piyush.swasthaireportgenerator.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TestCatalogUiState(
    val assignedTests: List<OrganizationTestResponse> = emptyList(),
    val parametersByTestRefId: Map<String, List<TestParameterResponse>> = emptyMap(),
    val loadingParametersForTest: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TestCatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val reportRepository = ReportRepository(ApiClient.getReportApi(application))
    private val dashboardRepository = DashboardRepository(ApiClient.getAnalyticsApi(application))

    private val _uiState = MutableStateFlow(TestCatalogUiState())
    val uiState: StateFlow<TestCatalogUiState> = _uiState.asStateFlow()

    fun loadAssignedTests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = reportRepository.getMyOrganizationTests()
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignedTests = list
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Failed to load organization tests"
                )
            }
        }
    }

    fun loadParametersForTest(testRefId: String) {
        if (_uiState.value.parametersByTestRefId.containsKey(testRefId) ||
            _uiState.value.loadingParametersForTest.contains(testRefId)
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingParametersForTest = _uiState.value.loadingParametersForTest + testRefId
            )
            val result = dashboardRepository.getTestParameters(testRefId)
            val currentLoading = _uiState.value.loadingParametersForTest - testRefId
            result.onSuccess { params ->
                val updatedMap = _uiState.value.parametersByTestRefId.toMutableMap()
                updatedMap[testRefId] = params
                _uiState.value = _uiState.value.copy(
                    loadingParametersForTest = currentLoading,
                    parametersByTestRefId = updatedMap
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(loadingParametersForTest = currentLoading)
            }
        }
    }
}
