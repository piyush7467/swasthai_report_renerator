package com.piyush.swasthaireportgenerator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object MainShell : Screen("main_shell")
    object TestCatalog : Screen("test_catalog")
    object StaffManagement : Screen("staff_management")
    object LicensePlans : Screen("license_plans")
    object OrgSettings : Screen("org_settings")
    object ThemeSettings : Screen("theme_settings")
    object AddPatient : Screen("add_patient")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsConditions : Screen("terms_conditions")
    object AboutApp : Screen("about_app")
    object HelpSupport : Screen("help_support")
    object StationSession : Screen("station_session/{userName}/{userRole}/{userEmail}") {
        fun createRoute(userName: String, userRole: String, userEmail: String): String {
            val encodedName = java.net.URLEncoder.encode(userName.ifBlank { "N/A" }, "UTF-8")
            val encodedRole = java.net.URLEncoder.encode(userRole.ifBlank { "N/A" }, "UTF-8")
            val encodedEmail = java.net.URLEncoder.encode(userEmail.ifBlank { "N/A" }, "UTF-8")
            return "station_session/$encodedName/$encodedRole/$encodedEmail"
        }
    }

    // Patient Clinical Profile & Report History
    object PatientProfile : Screen("patient_profile/{patientRefId}") {
        fun createRoute(patientRefId: String) = "patient_profile/$patientRefId"
    }

    // Multi-Step Report Creation & Workspace (Matches Web ReportWorkspacePage.tsx)
    object ReportWorkspace : Screen("report_workspace?reportRefId={reportRefId}&patientRefId={patientRefId}") {
        fun createRoute(reportRefId: String? = null, patientRefId: String? = null): String {
            val params = mutableListOf<String>()
            if (!reportRefId.isNullOrBlank()) params.add("reportRefId=$reportRefId")
            if (!patientRefId.isNullOrBlank()) params.add("patientRefId=$patientRefId")
            return if (params.isEmpty()) "report_workspace" else "report_workspace?${params.joinToString("&")}"
        }
    }

    // Finalized & Certified Report View (Matches Web FinalizedReportView.tsx)
    object FinalizedReport : Screen("finalized_report/{reportRefId}") {
        fun createRoute(reportRefId: String) = "finalized_report/$reportRefId"
    }
}

enum class BottomBarTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD(
        route = "tab_dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    PATIENTS(
        route = "tab_patients",
        title = "Patients",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    REPORTS(
        route = "tab_reports",
        title = "Worklist",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    ),
    SETTINGS(
        route = "tab_settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
