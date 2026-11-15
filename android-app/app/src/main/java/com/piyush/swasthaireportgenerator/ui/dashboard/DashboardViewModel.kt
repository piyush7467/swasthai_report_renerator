package com.piyush.swasthaireportgenerator.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.CategoryUsageStatsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrgActivityItemResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationLicenseOverviewResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.repository.DashboardRepository
import com.piyush.swasthaireportgenerator.data.repository.LabStaffRepository
import com.piyush.swasthaireportgenerator.data.repository.LicenseRepository
import com.piyush.swasthaireportgenerator.data.repository.PatientRepository
import com.piyush.swasthaireportgenerator.data.repository.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "LAB_STAFF",
    val organizationName: String = "",
    val organizationProfile: OrganizationProfileResponse? = null,
    val logoBytes: ByteArray? = null,
    val pendingCount: Long = 0,
    val finalizedCount: Long = 0,
    val patientCount: Long = 0,
    val reportsTodayCount: Long = 0,
    val recentDrafts: List<ReportResponse> = emptyList(),
    val recentPatients: List<PatientResponse> = emptyList(),
    val recentActivity: List<OrgActivityItemResponse> = emptyList(),
    val categoryUsage: List<CategoryUsageStatsResponse> = emptyList(),
    val staffSummary: LabStaffSummaryResponse? = null,
    val licenseInfo: LicenseResponse? = null,
    val licenseOverview: OrganizationLicenseOverviewResponse? = null,
    val licenseDaysRemaining: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val reportRepository = ReportRepository(ApiClient.getReportApi(application))
    private val patientRepository = PatientRepository(ApiClient.getPatientApi(application))
    private val dashboardRepository = DashboardRepository(ApiClient.getAnalyticsApi(application))
    private val staffRepository = LabStaffRepository(ApiClient.getLabStaffApi(application))
    private val licenseRepository = LicenseRepository(ApiClient.getLicenseApi(application))

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    userName = session.name,
                    userEmail = session.email,
                    userRole = session.role
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = DashboardUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first() ?: return@launch
            if (session.accessToken.isBlank()) return@launch

            val role = session.role
            _uiState.value = _uiState.value.copy(
                userName = session.name,
                userEmail = session.email,
                userRole = role,
                isLoading = true,
                errorMessage = null
            )

            coroutineScope {
                val profileDeferred = async { dashboardRepository.getOrganizationProfile() }
                val analyticsDeferred = async { dashboardRepository.getOverviewStats() }
                val draftsDeferred = async { reportRepository.getMyReports(status = "DRAFT") }
                val patientsDeferred = async { patientRepository.getPatients() }
                val activityDeferred = async { dashboardRepository.getRecentActivity() }
                val categoryDeferred = async { dashboardRepository.getCategoryUsage() }
                val staffDeferred = if (role.equals("ORG_ADMIN", ignoreCase = true)) {
                    async { staffRepository.getSummary() }
                } else null
                val overviewDeferred = if (role.equals("ORG_ADMIN", ignoreCase = true)) {
                    async { licenseRepository.getOverview() }
                } else null
                val ownLicenseDeferred = if (!role.equals("ORG_ADMIN", ignoreCase = true)) {
                    async { licenseRepository.getOwnLicense() }
                } else null

                val profileResult = profileDeferred.await()
                val analyticsResult = analyticsDeferred.await()
                val draftsResult = draftsDeferred.await()
                val patientsResult = patientsDeferred.await()
                val recentActivity = activityDeferred.await().getOrDefault(emptyList())
                val categoryUsage = categoryDeferred.await().getOrDefault(emptyList())

                val profile = profileResult.getOrNull()
                val logoBytes = if (profile?.logoConfigured == true) dashboardRepository.getLogoBytes() else null
                val drafts = draftsResult.getOrDefault(emptyList())
                val patientsList = patientsResult.getOrDefault(emptyList())
                val analytics = analyticsResult.getOrNull()

                val pending = analytics?.draftReports ?: drafts.size.toLong()
                val finalized = analytics?.finalizedReports
                    ?: reportRepository.getMyReports(status = "FINALIZED").getOrDefault(emptyList()).size.toLong()
                val patients = analytics?.totalPatients ?: patientsList.size.toLong()
                val today = analytics?.reportsToday ?: 0L

                var staffSummary: LabStaffSummaryResponse? = null
                var licenseInfo: LicenseResponse? = null
                var licenseOverview: OrganizationLicenseOverviewResponse? = null
                var daysRemaining: Long? = null

                if (role.equals("ORG_ADMIN", ignoreCase = true)) {
                    staffSummary = staffDeferred?.await()?.getOrNull()
                    licenseOverview = overviewDeferred?.await()?.getOrNull()
                    licenseInfo = licenseOverview?.license
                    daysRemaining = licenseOverview?.daysRemaining
                } else {
                    licenseInfo = ownLicenseDeferred?.await()?.getOrNull()
                }

                val firstError = analyticsResult.exceptionOrNull()?.localizedMessage
                    ?: profileResult.exceptionOrNull()?.localizedMessage
                    ?: draftsResult.exceptionOrNull()?.localizedMessage

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    organizationProfile = profile,
                    organizationName = profile?.organizationName ?: "",
                    logoBytes = logoBytes,
                    pendingCount = pending,
                    finalizedCount = finalized,
                    patientCount = patients,
                    reportsTodayCount = today,
                    recentDrafts = drafts.take(5),
                    recentPatients = patientsList.take(5),
                    recentActivity = recentActivity.take(6),
                    categoryUsage = categoryUsage,
                    staffSummary = staffSummary,
                    licenseInfo = licenseInfo,
                    licenseOverview = licenseOverview,
                    licenseDaysRemaining = daysRemaining,
                    errorMessage = if (analytics == null && profile == null && draftsResult.isFailure) {
                        firstError ?: "Failed to load dashboard data from server."
                    } else null
                )
            }
        }
    }
}
