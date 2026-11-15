package com.piyush.swasthaireportgenerator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.piyush.swasthaireportgenerator.ui.auth.AuthViewModel
import com.piyush.swasthaireportgenerator.ui.auth.LoginScreen
import com.piyush.swasthaireportgenerator.ui.auth.LoginUiState
import com.piyush.swasthaireportgenerator.ui.auth.SplashScreen
import com.piyush.swasthaireportgenerator.ui.dashboard.DashboardViewModel
import com.piyush.swasthaireportgenerator.ui.legal.AboutScreen
import com.piyush.swasthaireportgenerator.ui.legal.HelpSupportScreen
import com.piyush.swasthaireportgenerator.ui.legal.PrivacyPolicyScreen
import com.piyush.swasthaireportgenerator.ui.legal.TermsConditionsScreen
import com.piyush.swasthaireportgenerator.ui.license.LicenseScreen
import com.piyush.swasthaireportgenerator.ui.license.LicenseViewModel
import com.piyush.swasthaireportgenerator.ui.organization.OrganizationProfileViewModel
import com.piyush.swasthaireportgenerator.ui.organization.OrganizationSettingsScreen
import com.piyush.swasthaireportgenerator.ui.patients.AddPatientScreen
import com.piyush.swasthaireportgenerator.ui.patients.PatientProfileScreen
import com.piyush.swasthaireportgenerator.ui.patients.PatientViewModel
import com.piyush.swasthaireportgenerator.ui.reports.FinalizedReportViewScreen
import com.piyush.swasthaireportgenerator.ui.reports.ReportViewModel
import com.piyush.swasthaireportgenerator.ui.reports.ReportWorkspaceScreen
import com.piyush.swasthaireportgenerator.ui.settings.StationSessionScreen
import com.piyush.swasthaireportgenerator.ui.settings.ThemeSettingsScreen
import com.piyush.swasthaireportgenerator.ui.staff.StaffManagementScreen
import com.piyush.swasthaireportgenerator.ui.staff.StaffViewModel
import com.piyush.swasthaireportgenerator.ui.tests.TestCatalogScreen
import com.piyush.swasthaireportgenerator.ui.theme.ThemeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    onOpenThemeDialog: () -> Unit
) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val patientViewModel: PatientViewModel = viewModel()
    val reportViewModel: ReportViewModel = viewModel()
    val staffViewModel: StaffViewModel = viewModel()
    val licenseViewModel: LicenseViewModel = viewModel()
    val orgProfileViewModel: OrganizationProfileViewModel = viewModel()

    fun resetAllWorkspaceStates() {
        dashboardViewModel.resetState()
        patientViewModel.resetState()
        reportViewModel.resetState()
        staffViewModel.resetState()
        licenseViewModel.resetState()
        orgProfileViewModel.resetState()
    }

    LaunchedEffect(Unit) {
        authViewModel.sessionExpiredEvent.collect {
            resetAllWorkspaceStates()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // 0. SPLASH SCREEN
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    val currentState = authViewModel.uiState.value
                    val destination = if (currentState is LoginUiState.Success) {
                        Screen.MainShell.route
                    } else {
                        Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 1. LOGIN SCREEN (Full Screen Auth + Super Admin Guard)
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.MainShell.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onOpenThemeDialog = onOpenThemeDialog
            )
        }

        // 2. MAIN APPLICATION SHELL (Sidebar Drawer + Bottom Navigation Bar)
        composable(Screen.MainShell.route) {
            MainAppShell(
                authViewModel = authViewModel,
                dashboardViewModel = dashboardViewModel,
                patientViewModel = patientViewModel,
                reportViewModel = reportViewModel,
                themeViewModel = themeViewModel,
                onNavigateToReportWorkspace = { reportRefId, patientRefId ->
                    navController.navigate(Screen.ReportWorkspace.createRoute(reportRefId, patientRefId))
                },
                onNavigateToFinalizedReport = { reportRefId ->
                    navController.navigate(Screen.FinalizedReport.createRoute(reportRefId))
                },
                onNavigateToPatientProfile = { patientRefId ->
                    navController.navigate(Screen.PatientProfile.createRoute(patientRefId))
                },
                onNavigateToTestCatalog = {
                    navController.navigate(Screen.TestCatalog.route)
                },
                onNavigateToStaff = {
                    navController.navigate(Screen.StaffManagement.route)
                },
                onNavigateToLicense = {
                    navController.navigate(Screen.LicensePlans.route)
                },
                onNavigateToOrgSettings = {
                    navController.navigate(Screen.OrgSettings.route)
                },
                onNavigateToThemeSettings = {
                    navController.navigate(Screen.ThemeSettings.route)
                },
                onNavigateToStationSession = { name, role, email ->
                    navController.navigate(Screen.StationSession.createRoute(name, role, email))
                },
                onNavigateToAddPatient = {
                    navController.navigate(Screen.AddPatient.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsConditions.route)
                },
                onNavigateToAboutApp = {
                    navController.navigate(Screen.AboutApp.route)
                },
                onNavigateToHelpSupport = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onLogout = {
                    resetAllWorkspaceStates()
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 3. FIVE-STEP CLINICAL REPORT WORKSPACE (Matches Web ReportWorkspacePage.tsx)
        composable(
            route = Screen.ReportWorkspace.route,
            arguments = listOf(
                navArgument("reportRefId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("patientRefId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val reportRefId = backStackEntry.arguments?.getString("reportRefId")
            val patientRefId = backStackEntry.arguments?.getString("patientRefId")
            ReportWorkspaceScreen(
                initialReportRefId = reportRefId,
                initialPatientRefId = patientRefId,
                reportViewModel = reportViewModel,
                patientViewModel = patientViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFinalized = { finalizedRefId ->
                    dashboardViewModel.refreshDashboard()
                    navController.navigate(Screen.FinalizedReport.createRoute(finalizedRefId)) {
                        popUpTo(Screen.ReportWorkspace.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. FINALIZED & CERTIFIED REPORT VIEW (Matches Web FinalizedReportView.tsx)
        composable(
            route = Screen.FinalizedReport.route,
            arguments = listOf(navArgument("reportRefId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportRefId = backStackEntry.arguments?.getString("reportRefId") ?: ""
            FinalizedReportViewScreen(
                reportRefId = reportRefId,
                viewModel = reportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. CLINICAL TEST CATALOG & REFERENCE RANGES (Matches Web AssignedTestsPage.tsx)
        composable(Screen.TestCatalog.route) {
            TestCatalogScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateNewReport = { navController.navigate(Screen.ReportWorkspace.createRoute()) }
            )
        }

        // 6. LAB STAFF MANAGEMENT (ORG_ADMIN, Matches Web LabStaffPage.tsx)
        composable(Screen.StaffManagement.route) {
            StaffManagementScreen(
                viewModel = staffViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlans = { navController.navigate(Screen.LicensePlans.route) }
            )
        }

        // 7. PLANS & SUBSCRIPTION / LICENSING (Matches Web OrganizationLicensePage.tsx)
        composable(Screen.LicensePlans.route) {
            LicenseScreen(
                viewModel = licenseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 8. PATIENT PROFILE & REPORT HISTORY (Matches Web PatientProfilePage.tsx)
        composable(
            route = Screen.PatientProfile.route,
            arguments = listOf(navArgument("patientRefId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientRefId = backStackEntry.arguments?.getString("patientRefId") ?: ""
            PatientProfileScreen(
                patientRefId = patientRefId,
                viewModel = patientViewModel,
                onNavigateBack = {
                    dashboardViewModel.refreshDashboard()
                    navController.popBackStack()
                },
                onCreateReportForPatient = { patRefId ->
                    navController.navigate(Screen.ReportWorkspace.createRoute(patientRefId = patRefId))
                },
                onNavigateToReportEntry = { reportRefId ->
                    navController.navigate(Screen.ReportWorkspace.createRoute(reportRefId = reportRefId))
                },
                onNavigateToFinalizedReport = { reportRefId ->
                    navController.navigate(Screen.FinalizedReport.createRoute(reportRefId))
                }
            )
        }

        // 9. ORGANIZATION PROFILE & BRANDING SETTINGS (Matches Web OrganizationSettingsPage.tsx)
        composable(Screen.OrgSettings.route) {
            OrganizationSettingsScreen(
                viewModel = orgProfileViewModel,
                onNavigateBack = {
                    dashboardViewModel.refreshDashboard()
                    navController.popBackStack()
                }
            )
        }

        // 10. THEME SETTINGS SCREEN
        composable(Screen.ThemeSettings.route) {
            ThemeSettingsScreen(
                themeViewModel = themeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 11. STATION SESSION SCREEN
        composable(
            route = Screen.StationSession.route,
            arguments = listOf(
                navArgument("userName") { type = NavType.StringType },
                navArgument("userRole") { type = NavType.StringType },
                navArgument("userEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("userName") ?: ""
            val encodedRole = backStackEntry.arguments?.getString("userRole") ?: ""
            val encodedEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            val name = java.net.URLDecoder.decode(encodedName, "UTF-8")
            val role = java.net.URLDecoder.decode(encodedRole, "UTF-8")
            val email = java.net.URLDecoder.decode(encodedEmail, "UTF-8")
            StationSessionScreen(
                userName = name,
                userRole = role,
                userEmail = email,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 12. ADD PATIENT SCREEN
        composable(Screen.AddPatient.route) {
            AddPatientScreen(
                viewModel = patientViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPatientCreated = {
                    dashboardViewModel.refreshDashboard()
                    patientViewModel.loadPatients()
                    navController.popBackStack()
                }
            )
        }

        // 13. PRIVACY POLICY
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 14. TERMS & CONDITIONS
        composable(Screen.TermsConditions.route) {
            TermsConditionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 15. ABOUT APP
        composable(Screen.AboutApp.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 16. HELP & SUPPORT
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
