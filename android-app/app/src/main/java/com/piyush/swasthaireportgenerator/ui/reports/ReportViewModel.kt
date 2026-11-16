package com.piyush.swasthaireportgenerator.ui.reports

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationTestResponse
import com.piyush.swasthaireportgenerator.data.model.ParameterValueUpdate
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.ReportShareResponse
import com.piyush.swasthaireportgenerator.data.repository.DashboardRepository
import com.piyush.swasthaireportgenerator.data.repository.LabStaffRepository
import com.piyush.swasthaireportgenerator.data.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReportListUiState(
    val reports: List<ReportResponse> = emptyList(),
    val userRole: String = "LAB_STAFF",
    val activeFilter: String? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

data class ReportEntryUiState(
    val report: ReportResponse? = null,
    val organizationProfile: OrganizationProfileResponse? = null,
    val signatoryOptions: List<String> = emptyList(),
    val availableTests: List<OrganizationTestResponse> = emptyList(),
    val isTestsLoading: Boolean = false,
    val testsErrorMessage: String? = null,
    val parameterValues: Map<String, String> = emptyMap(),
    val logoBytes: ByteArray? = null,
    val signatureBytes: ByteArray? = null,
    val qrBytes: ByteArray? = null,
    val isAssetsLoading: Boolean = false,
    val isPdfDownloading: Boolean = false,
    val pdfError: String? = null,
    val isSharingLink: Boolean = false,
    val shareResponse: ReportShareResponse? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRecalculating: Boolean = false,
    val isFinalizing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val reportRepository = ReportRepository(ApiClient.getReportApi(application))
    private val dashboardRepository = DashboardRepository(ApiClient.getAnalyticsApi(application))
    private val staffRepository = LabStaffRepository(ApiClient.getLabStaffApi(application))
    private val tokenManager = TokenManager(application)

    private val _listState = MutableStateFlow(ReportListUiState())
    val listState: StateFlow<ReportListUiState> = _listState.asStateFlow()

    private val _entryState = MutableStateFlow(ReportEntryUiState())
    val entryState: StateFlow<ReportEntryUiState> = _entryState.asStateFlow()

    fun resetState() {
        _listState.value = ReportListUiState()
        _entryState.value = ReportEntryUiState()
    }

    fun resetEntryState() {
        _entryState.value = ReportEntryUiState(
            availableTests = _entryState.value.availableTests,
            organizationProfile = _entryState.value.organizationProfile,
            signatoryOptions = _entryState.value.signatoryOptions
        )
    }

    fun loadWorkspaceContext() {
        loadOrganizationTests()
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            val profile = dashboardRepository.getOrganizationProfile().getOrNull()

            val options = linkedSetOf<String>()
            val sigName = profile?.signatureOwnerName?.trim()
            if (!sigName.isNullOrBlank()) {
                options.add("$sigName (Authorized Signatory)")
            }
            if (session != null && session.name.isNotBlank()) {
                options.add("${session.name} (${session.role})")
            }
            if (session?.role.equals("ORG_ADMIN", ignoreCase = true)) {
                val activeStaff = staffRepository.getStaffList(status = "ACTIVE").getOrDefault(emptyList())
                activeStaff.forEach { staff ->
                    options.add("${staff.name} (Lab Staff)")
                }
            }
            if (options.isEmpty()) {
                options.add("Self / Walk-in")
            } else {
                options.add("Self / Walk-in")
            }

            _entryState.value = _entryState.value.copy(
                organizationProfile = profile,
                signatoryOptions = options.toList()
            )
        }
    }

    fun loadOrganizationTests() {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isTestsLoading = true,
                testsErrorMessage = null
            )
            val result = reportRepository.getMyOrganizationTests()
            result.onSuccess { tests ->
                _entryState.value = _entryState.value.copy(
                    isTestsLoading = false,
                    availableTests = tests
                )
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isTestsLoading = false,
                    testsErrorMessage = error.localizedMessage ?: "Failed to load lab tests"
                )
            }
        }
    }

    fun loadReports(status: String? = _listState.value.activeFilter) {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            if (session == null || session.accessToken.isBlank()) return@launch
            val role = session.role.ifBlank { "LAB_STAFF" }
            _listState.value = _listState.value.copy(
                isLoading = true,
                activeFilter = status,
                userRole = role,
                errorMessage = null
            )
            val result = reportRepository.getMyReports(status = status)
            result.onSuccess { list ->
                _listState.value = _listState.value.copy(isLoading = false, reports = list)
            }.onFailure { error ->
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load reports"
                )
            }
        }
    }

    fun deleteReport(reportRefId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isDeleting = true, errorMessage = null)
            val result = reportRepository.deleteReport(reportRefId)
            result.onSuccess {
                _listState.value = _listState.value.copy(isDeleting = false)
                loadReports(_listState.value.activeFilter)
                onSuccess()
            }.onFailure { error ->
                _listState.value = _listState.value.copy(
                    isDeleting = false,
                    errorMessage = error.localizedMessage ?: "Failed to delete report"
                )
            }
        }
    }

    fun clearListError() {
        _listState.value = _listState.value.copy(errorMessage = null)
    }

    private fun extractParamMap(report: ReportResponse): Map<String, String> {
        val valuesMap = mutableMapOf<String, String>()
        report.tests.forEach { test ->
            test.parameters.forEach { param ->
                if (!param.value.isNullOrBlank()) {
                    valuesMap[param.parameterRefId] = param.value
                }
            }
        }
        return ClinicalCalculations.computeAllCalculations(report, valuesMap)
    }

    private fun mergeParamMap(report: ReportResponse, currentMap: Map<String, String>): Map<String, String> {
        val validRefIds = report.tests.flatMap { it.parameters }.map { it.parameterRefId }.toSet()
        val merged = currentMap.filterKeys { it in validRefIds }.toMutableMap()
        for (test in report.tests) {
            for (param in test.parameters) {
                if (merged[param.parameterRefId].isNullOrBlank()) {
                    if (!param.value.isNullOrBlank()) {
                        merged[param.parameterRefId] = param.value
                    }
                }
            }
        }
        return ClinicalCalculations.computeAllCalculations(report, merged)
    }

    fun silentAutoSaveParameters() {
        val currentReport = _entryState.value.report ?: return
        viewModelScope.launch {
            persistAllManualParameters(currentReport)
        }
    }

    fun loadReport(reportRefId: String) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isLoading = true,
                errorMessage = null,
                statusMessage = null
            )
            val result = reportRepository.getReport(reportRefId)
            result.onSuccess { report ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    report = report,
                    parameterValues = mergeParamMap(report, _entryState.value.parameterValues)
                )
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load report"
                )
            }
        }
    }

    fun onParameterChange(parameterRefId: String, value: String) {
        val updatedMap = _entryState.value.parameterValues.toMutableMap()
        updatedMap[parameterRefId] = value

        // Automatically compute standard clinical calculated parameters (MCV, MCH, MCHC, etc.) in real time
        val computedMap = _entryState.value.report?.let { report ->
            ClinicalCalculations.computeAllCalculations(report, updatedMap)
        } ?: updatedMap

        _entryState.value = _entryState.value.copy(
            parameterValues = computedMap,
            errorMessage = null
        )
    }

    fun createReportWithTests(
        patientRefId: String,
        testRefIds: List<String>,
        onSuccess: (ReportResponse) -> Unit
    ) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isLoading = true,
                errorMessage = null,
                statusMessage = null
            )
            val result = reportRepository.createReportWithTests(patientRefId, testRefIds)
            result.onSuccess { newReport ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    report = newReport,
                    parameterValues = extractParamMap(newReport)
                )
                loadReports()
                onSuccess(newReport)
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to create report"
                )
            }
        }
    }

    fun addTestsToExistingReport(
        reportRefId: String,
        newTestRefIds: List<String>,
        onSuccess: (ReportResponse) -> Unit
    ) {
        viewModelScope.launch {
            val currentReport = _entryState.value.report
            if (newTestRefIds.isEmpty()) {
                if (currentReport != null) onSuccess(currentReport)
                return@launch
            }

            _entryState.value = _entryState.value.copy(isLoading = true, errorMessage = null)

            // Auto-persist already-entered parameters so the server draft also contains them
            if (currentReport != null) {
                persistAllManualParameters(currentReport)
            }

            val result = reportRepository.addTestsBulk(
                reportRefId = reportRefId,
                testRefIds = newTestRefIds,
                lockVersion = _entryState.value.report?.lockVersion ?: currentReport?.lockVersion
            )
            result.onSuccess { updatedReport ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    report = updatedReport,
                    parameterValues = mergeParamMap(updatedReport, _entryState.value.parameterValues)
                )
                onSuccess(updatedReport)
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to add selected tests"
                )
            }
        }
    }

    private suspend fun persistAllManualParameters(report: ReportResponse): Result<ReportResponse> {
        var latestReport = report
        val enteredValues = _entryState.value.parameterValues

        for (test in latestReport.tests) {
            val manualUpdates = test.parameters
                .filter { !it.inputType.equals("CALCULATED", ignoreCase = true) }
                .mapNotNull { param ->
                    val entered = enteredValues[param.parameterRefId]?.trim() ?: ""
                    val original = param.value?.trim() ?: ""
                    if (entered != original || entered.isNotEmpty()) {
                        ParameterValueUpdate(
                            parameterRefId = param.parameterRefId,
                            parameterCode = param.parameterCode,
                            value = entered
                        )
                    } else null
                }

            if (manualUpdates.isNotEmpty()) {
                val saveRes = reportRepository.updateParameters(
                    reportRefId = latestReport.refId,
                    reportTestRefId = test.refId,
                    lockVersion = latestReport.lockVersion,
                    updates = manualUpdates
                )
                if (saveRes.isFailure) {
                    return saveRes
                }
                latestReport = saveRes.getOrThrow()
            }
        }

        return Result.success(latestReport)
    }

    fun saveAndRecalculate(reportRefId: String) {
        viewModelScope.launch {
            val currentReport = _entryState.value.report ?: return@launch
            _entryState.value = _entryState.value.copy(
                isRecalculating = true,
                errorMessage = null,
                statusMessage = null
            )

            val saveResult = persistAllManualParameters(currentReport)
            if (saveResult.isFailure) {
                _entryState.value = _entryState.value.copy(
                    isRecalculating = false,
                    errorMessage = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save values"
                )
                return@launch
            }

            val recalcResult = reportRepository.recalculate(reportRefId)
            recalcResult.onSuccess { recalculatedReport ->
                _entryState.value = _entryState.value.copy(
                    isRecalculating = false,
                    report = recalculatedReport,
                    parameterValues = extractParamMap(recalculatedReport),
                    statusMessage = "Values saved & recalculated!"
                )
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isRecalculating = false,
                    errorMessage = error.localizedMessage ?: "Failed to recalculate"
                )
            }
        }
    }

    fun saveAndProceedToReview(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentReport = _entryState.value.report
            if (currentReport == null) {
                onSuccess()
                return@launch
            }
            _entryState.value = _entryState.value.copy(isSaving = true, errorMessage = null)
            val saveResult = persistAllManualParameters(currentReport)
            saveResult.onSuccess { savedReport ->
                val recalcResult = reportRepository.recalculate(savedReport.refId)
                val finalPreviewReport = recalcResult.getOrDefault(savedReport)
                _entryState.value = _entryState.value.copy(
                    isSaving = false,
                    report = finalPreviewReport,
                    parameterValues = extractParamMap(finalPreviewReport)
                )
                onSuccess()
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isSaving = false,
                    errorMessage = error.localizedMessage ?: "Failed to save parameters"
                )
            }
        }
    }

    fun updateHeaderOption(reportRefId: String, includeHeader: Boolean) {
        viewModelScope.launch {
            val result = reportRepository.updateHeaderOption(reportRefId, includeHeader)
            result.onSuccess { updatedReport ->
                _entryState.value = _entryState.value.copy(report = updatedReport)
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    errorMessage = error.localizedMessage ?: "Failed to update letterhead setting"
                )
            }
        }
    }

    fun finalizeReport(reportRefId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isFinalizing = true,
                errorMessage = null,
                statusMessage = null
            )
            val result = reportRepository.finalizeReport(reportRefId)
            result.onSuccess { finalReport ->
                _entryState.value = _entryState.value.copy(
                    isFinalizing = false,
                    report = finalReport,
                    parameterValues = extractParamMap(finalReport),
                    statusMessage = "Report finalized and electronically signed!"
                )
                loadReports()
                onSuccess()
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isFinalizing = false,
                    errorMessage = error.localizedMessage ?: "Failed to finalize report"
                )
            }
        }
    }

    fun removeTest(reportRefId: String, reportTestRefId: String) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(isSaving = true, errorMessage = null, statusMessage = null)
            val result = reportRepository.removeTest(reportRefId, reportTestRefId)
            result.onSuccess { updatedReport ->
                _entryState.value = _entryState.value.copy(
                    isSaving = false,
                    report = updatedReport,
                    parameterValues = mergeParamMap(updatedReport, _entryState.value.parameterValues),
                    statusMessage = "Test panel removed from report."
                )
            }.onFailure { error ->
                _entryState.value = _entryState.value.copy(
                    isSaving = false,
                    errorMessage = error.localizedMessage ?: "Failed to remove test from report"
                )
            }
        }
    }

    fun loadFinalizedAssets(reportRefId: String) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isAssetsLoading = true,
                pdfError = null
            )
            val profile = _entryState.value.organizationProfile
                ?: dashboardRepository.getOrganizationProfile().getOrNull()
            val logo = if (profile?.logoConfigured == true) dashboardRepository.getLogoBytes() else null
            val sig = if (profile?.signatureConfigured == true) dashboardRepository.getSignatureBytes() else null
            val qr = reportRepository.getQrBytes(reportRefId)

            _entryState.value = _entryState.value.copy(
                organizationProfile = profile,
                logoBytes = logo,
                signatureBytes = sig,
                qrBytes = qr,
                isAssetsLoading = false
            )
        }
    }

    fun downloadPdfAndGetUri(
        context: Context,
        reportRefId: String,
        includeHeader: Boolean,
        onUriReady: (Uri) -> Unit
    ) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(isPdfDownloading = true, pdfError = null)
            val result = reportRepository.downloadPdfBytes(reportRefId, includeHeader)
            result.fold(
                onSuccess = { bytes ->
                    val uri = withContext(Dispatchers.IO) {
                        val reportsDir = File(context.cacheDir, "reports")
                        if (!reportsDir.exists()) reportsDir.mkdirs()
                        val code = reportRefId.replace(Regex("[^A-Za-z0-9._-]"), "_")
                        val pdfFile = File(reportsDir, "SwasthReport_$code.pdf")
                        pdfFile.writeBytes(bytes)
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                    }
                    _entryState.value = _entryState.value.copy(isPdfDownloading = false)
                    onUriReady(uri)
                },
                onFailure = { error ->
                    _entryState.value = _entryState.value.copy(
                        isPdfDownloading = false,
                        pdfError = error.localizedMessage ?: "Failed to download official PDF"
                    )
                }
            )
        }
    }

    fun generateShareLink(
        reportRefId: String,
        channel: String = "LINK",
        expiresInDays: Int = 7
    ) {
        viewModelScope.launch {
            _entryState.value = _entryState.value.copy(
                isSharingLink = true,
                pdfError = null
            )
            val result = reportRepository.createShareLink(
                reportRefId = reportRefId,
                channel = channel,
                expiresInDays = expiresInDays
            )
            result.fold(
                onSuccess = { share ->
                    _entryState.value = _entryState.value.copy(
                        isSharingLink = false,
                        shareResponse = share
                    )
                },
                onFailure = { error ->
                    _entryState.value = _entryState.value.copy(
                        isSharingLink = false,
                        pdfError = error.localizedMessage ?: "Failed to create share link"
                    )
                }
            )
        }
    }

    fun clearShareState() {
        _entryState.value = _entryState.value.copy(shareResponse = null)
    }

    fun clearPdfError() {
        _entryState.value = _entryState.value.copy(pdfError = null)
    }
}
