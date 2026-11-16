package com.piyush.swasthaireportgenerator.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.piyush.swasthaireportgenerator.ui.auth.AuthViewModel
import com.piyush.swasthaireportgenerator.ui.components.AppBottomBar
import com.piyush.swasthaireportgenerator.ui.components.AppDrawerContent
import com.piyush.swasthaireportgenerator.ui.components.ThemeSelectorDialog
import com.piyush.swasthaireportgenerator.ui.dashboard.DashboardScreen
import com.piyush.swasthaireportgenerator.ui.dashboard.DashboardViewModel
import com.piyush.swasthaireportgenerator.ui.patients.PatientListScreen
import com.piyush.swasthaireportgenerator.ui.patients.PatientViewModel
import com.piyush.swasthaireportgenerator.ui.reports.ReportListScreen
import com.piyush.swasthaireportgenerator.ui.reports.ReportViewModel
import com.piyush.swasthaireportgenerator.ui.settings.SettingsScreen
import com.piyush.swasthaireportgenerator.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
fun MainAppShell(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    patientViewModel: PatientViewModel,
    reportViewModel: ReportViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateToReportWorkspace: (reportRefId: String?, patientRefId: String?) -> Unit,
    onNavigateToFinalizedReport: (reportRefId: String) -> Unit,
    onNavigateToPatientProfile: (patientRefId: String) -> Unit = {},
    onNavigateToTestCatalog: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToOrgSettings: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToStationSession: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToAddPatient: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(BottomBarTab.DASHBOARD) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val patientState by patientViewModel.uiState.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()

    // Scroll direction detection: show FAB when at top / scrolling up, hide when scrolling down
    var showFab by remember { mutableStateOf(true) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 2f) {
                    showFab = true
                } else if (available.y < -2f) {
                    showFab = false
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(currentTab) {
        showFab = true
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.refreshDashboard()
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentMode = themeState.mode,
            currentPalette = themeState.palette,
            currentAmoledDark = themeState.amoledDark,
            onModeSelected = { themeViewModel.setThemeMode(it) },
            onPaletteSelected = { themeViewModel.setThemePalette(it) },
            onAmoledDarkChanged = { themeViewModel.setAmoledDark(it) },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("Are you sure you want to end your laboratory session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentTab = currentTab,
                userName = dashboardState.userName,
                userRole = dashboardState.userRole,
                userEmail = dashboardState.userEmail,
                organizationName = dashboardState.organizationName,
                logoBytes = dashboardState.logoBytes,
                onTabSelected = { tab ->
                    if (tab == currentTab) {
                        when (tab) {
                            BottomBarTab.DASHBOARD -> dashboardViewModel.refreshDashboard()
                            BottomBarTab.PATIENTS -> patientViewModel.loadPatients()
                            BottomBarTab.REPORTS -> reportViewModel.loadReports()
                            BottomBarTab.SETTINGS -> dashboardViewModel.refreshDashboard()
                        }
                    } else {
                        currentTab = tab
                    }
                },
                onNavigateToTestCatalog = onNavigateToTestCatalog,
                onNavigateToStaff = onNavigateToStaff,
                onNavigateToLicense = onNavigateToLicense,
                onNavigateToOrgSettings = onNavigateToOrgSettings,
                onOpenThemeDialog = { showThemeDialog = true },
                onLogoutClick = { showLogoutDialog = true },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollConnection),
            bottomBar = {
                AppBottomBar(
                    currentTab = currentTab,
                    pendingReportsCount = dashboardState.pendingCount,
                    onTabSelected = { tab ->
                        if (tab == currentTab) {
                            when (tab) {
                                BottomBarTab.DASHBOARD -> dashboardViewModel.refreshDashboard()
                                BottomBarTab.PATIENTS -> patientViewModel.loadPatients()
                                BottomBarTab.REPORTS -> reportViewModel.loadReports()
                                BottomBarTab.SETTINGS -> dashboardViewModel.refreshDashboard()
                            }
                        } else {
                            currentTab = tab
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFab,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    when (currentTab) {
                        BottomBarTab.PATIENTS -> {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    patientViewModel.clearCreateError()
                                    onNavigateToAddPatient()
                                },
                                icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add") },
                                text = { Text("New Patient") },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        BottomBarTab.REPORTS -> {
                            ExtendedFloatingActionButton(
                                onClick = { onNavigateToReportWorkspace(null, null) },
                                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "New Report") },
                                text = { Text("New Report") },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        BottomBarTab.DASHBOARD -> {
                            FloatingActionButton(
                                onClick = { onNavigateToReportWorkspace(null, null) },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Report")
                            }
                        }
                        else -> {}
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                when (currentTab) {
                    BottomBarTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onNavigateToPatients = { currentTab = BottomBarTab.PATIENTS },
                            onNavigateToReports = { currentTab = BottomBarTab.REPORTS },
                            onNavigateToReportEntry = { reportRefId ->
                                onNavigateToReportWorkspace(reportRefId, null)
                            },
                            onNavigateToStaff = onNavigateToStaff,
                            onNavigateToLicense = onNavigateToLicense,
                            onNavigateToTestCatalog = onNavigateToTestCatalog,
                            onNavigateToOrgSettings = onNavigateToOrgSettings,
                            onOpenThemeDialog = { showThemeDialog = true },
                            onCreateReportClick = { onNavigateToReportWorkspace(null, null) }
                        )
                    }
                    BottomBarTab.PATIENTS -> {
                        PatientListScreen(
                            viewModel = patientViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenThemeDialog = { showThemeDialog = true },
                            onPatientClick = { patient ->
                                onNavigateToPatientProfile(patient.refId)
                            },
                            onNewTestClick = { patient ->
                                onNavigateToReportWorkspace(null, patient.refId)
                            }
                        )
                    }
                    BottomBarTab.REPORTS -> {
                        ReportListScreen(
                            viewModel = reportViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onNavigateToReportEntry = { reportRefId ->
                                onNavigateToReportWorkspace(reportRefId, null)
                            },
                            onNavigateToReportDetails = onNavigateToFinalizedReport,
                            onOpenThemeDialog = { showThemeDialog = true },
                            onCreateReportClick = { onNavigateToReportWorkspace(null, null) }
                        )
                    }
                    BottomBarTab.SETTINGS -> {
                        SettingsScreen(
                            themeViewModel = themeViewModel,
                            userName = dashboardState.userName,
                            userRole = dashboardState.userRole,
                            userEmail = dashboardState.userEmail,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onLogoutClick = { showLogoutDialog = true },
                            onNavigateToThemeSettings = onNavigateToThemeSettings,
                            onNavigateToStationSession = {
                                onNavigateToStationSession(
                                    dashboardState.userName,
                                    dashboardState.userRole,
                                    dashboardState.userEmail
                                )
                            },
                            onNavigateToOrgSettings = onNavigateToOrgSettings,
                            onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                            onNavigateToTerms = onNavigateToTerms,
                            onNavigateToAboutApp = onNavigateToAboutApp,
                            onNavigateToHelpSupport = onNavigateToHelpSupport
                        )
                    }
                }
            }
        }
    }
}
