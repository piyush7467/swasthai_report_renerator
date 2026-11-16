package com.piyush.swasthaireportgenerator.ui.dashboard

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.R
import com.piyush.swasthaireportgenerator.data.model.CategoryUsageStatsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrgActivityItemResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.ui.components.SkeletonBox
import com.piyush.swasthaireportgenerator.ui.components.SkeletonWorklistList

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReportEntry: (reportRefId: String) -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToTestCatalog: () -> Unit,
    onNavigateToOrgSettings: () -> Unit = {},
    onOpenThemeDialog: () -> Unit,
    onCreateReportClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOrgAdmin = uiState.userRole.equals("ORG_ADMIN", ignoreCase = true)
    var licenseBlockedMessage by remember { mutableStateOf<String?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val logoBitmap = remember(uiState.logoBytes) {
        uiState.logoBytes?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val license = uiState.licenseInfo
    val reportUsage = uiState.licenseOverview?.reportUsage
    val blockReason = when {
        license != null && !license.currentlyUsable ->
            "Your laboratory subscription (${license.status ?: "INACTIVE"}) is not currently active. Renew or upgrade your plan to generate new diagnostic reports."
        reportUsage?.dailyLimitReached == true ->
            "Daily report generation quota (${reportUsage.maxReportsPerDay} reports/day) has been reached on the current plan."
        reportUsage?.monthlyLimitReached == true ->
            "Monthly report generation quota (${reportUsage.maxReportsPerMonth} reports/month) has been reached. Upgrade your plan to continue."
        else -> null
    }

    if (licenseBlockedMessage != null) {
        AlertDialog(
            onDismissRequest = { licenseBlockedMessage = null },
            title = { Text("Subscription Limit Reached") },
            text = { Text(licenseBlockedMessage!!) },
            confirmButton = {
                Button(
                    onClick = {
                        licenseBlockedMessage = null
                        onNavigateToLicense()
                    }
                ) {
                    Text(if (isOrgAdmin) "View Plans & Upgrade" else "View License Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { licenseBlockedMessage = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Row(
                        modifier = Modifier.padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = "Lab Logo",
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_swasthai_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.organizationName.ifBlank { "SwasthAI Laboratory" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Sidebar Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Dashboard Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.refreshDashboard()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Letterhead & Branding") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToOrgSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Test Catalog") },
                                leadingIcon = { Icon(Icons.Default.Biotech, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToTestCatalog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Subscription & Limits") },
                                leadingIcon = { Icon(Icons.Default.CardMembership, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToLicense()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Theme Settings") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenThemeDialog()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshDashboard() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.refreshDashboard() }) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 0. Subscription Alert Banner
                val expiryStatus = uiState.licenseOverview?.expiryStatus?.uppercase()
                val showSubAlert = license != null && (
                    !license.currentlyUsable ||
                        reportUsage?.dailyLimitReached == true ||
                        reportUsage?.monthlyLimitReached == true ||
                        expiryStatus in setOf("EXPIRING_SOON", "EXPIRED", "SUSPENDED") ||
                        (uiState.licenseDaysRemaining != null && uiState.licenseDaysRemaining!! <= 7)
                    )
                if (showSubAlert) {
                    val isCritical = !license!!.currentlyUsable || expiryStatus in setOf("EXPIRED", "SUSPENDED")
                    val bannerTitle = when {
                        !license.currentlyUsable -> "Subscription ${license.status ?: "Inactive"}"
                        reportUsage?.monthlyLimitReached == true -> "Monthly Report Quota Reached"
                        reportUsage?.dailyLimitReached == true -> "Daily Report Quota Reached"
                        uiState.licenseDaysRemaining != null -> "Plan Expiring in ${uiState.licenseDaysRemaining} Days"
                        else -> "Subscription Attention Needed"
                    }
                    val bannerSub = when {
                        blockReason != null -> blockReason
                        else -> "${license.planName ?: "Current Plan"} ends soon. Review plans and billing to avoid downtime."
                    }
                    item {
                        Surface(
                            color = if (isCritical) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateToLicense)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCritical) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                    contentDescription = "Subscription Alert",
                                    tint = if (isCritical) MaterialTheme.colorScheme.error else Color(0xFFB45309)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bannerTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCritical) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF78350F)
                                    )
                                    Text(
                                        text = bannerSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCritical) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF92400E)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open License",
                                    tint = if (isCritical) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }

                // 1. Hero Banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp)),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 3.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF064E3B),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34D399))
                                            )
                                            Text(
                                                text = if (isOrgAdmin) "Admin Console" else "Laboratory Station",
                                                color = Color(0xFF6EE7B7),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = uiState.userRole,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Welcome, ${uiState.userName.ifBlank { "Team" }}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Text(
                                    text = "${uiState.reportsTodayCount} reports generated today • Clinical intake, parameter entry, and electronic certification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (blockReason != null) {
                                                licenseBlockedMessage = blockReason
                                            } else {
                                                onCreateReportClick()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Report",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = onNavigateToPatients,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Patients",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF6EE7B7)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Patients", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Comprehensive Statistics Cards (Row 1: 3 cards, Row 2: 2 cards)
                val isStatsLoading = uiState.isLoading && uiState.pendingCount == 0L && uiState.finalizedCount == 0L
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Row 1: 3 cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardGridStatCard(
                                title = "TOTAL REPORTS",
                                value = (uiState.licenseOverview?.reportUsage?.totalReports ?: (uiState.pendingCount + uiState.finalizedCount)).toString(),
                                subtitle = "All generated reports",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                modifier = Modifier.weight(1f),
                                isLoading = isStatsLoading,
                                onClick = onNavigateToReports
                            )

                            DashboardGridStatCard(
                                title = "REPORTS TODAY",
                                value = uiState.reportsTodayCount.toString(),
                                subtitle = "24h cycle",
                                icon = Icons.Default.Today,
                                modifier = Modifier.weight(1f),
                                isLoading = isStatsLoading,
                                onClick = onNavigateToReports
                            )

                            DashboardGridStatCard(
                                title = "FINALIZED",
                                value = uiState.finalizedCount.toString(),
                                subtitle = "Certified",
                                icon = Icons.Default.Verified,
                                modifier = Modifier.weight(1f),
                                isLoading = isStatsLoading,
                                onClick = onNavigateToReports
                            )
                        }

                        // Row 2: 2 cards + spacer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardGridStatCard(
                                title = "TOTAL PATIENTS",
                                value = uiState.patientCount.toString(),
                                subtitle = "Registered profiles",
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f),
                                isLoading = isStatsLoading,
                                onClick = onNavigateToPatients
                            )

                            DashboardGridStatCard(
                                title = "ACTIVE TESTS",
                                value = (uiState.licenseOverview?.reportUsage?.maxReportsPerMonth?.let { "$it Limit" } ?: "Active"),
                                subtitle = "Test parameters",
                                icon = Icons.Default.Biotech,
                                modifier = Modifier.weight(1f),
                                isLoading = isStatsLoading,
                                onClick = onNavigateToTestCatalog
                            )

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 3. Lab Letterhead & Branding Status Card
                item {
                    DashboardBrandingStatusCard(
                        profile = uiState.organizationProfile,
                        onClick = onNavigateToOrgSettings
                    )
                }

                // 4. Staff Capacity & Plan Card
                item {
                    DashboardStaffCapacityCard(
                        staffSummary = uiState.staffSummary,
                        licenseInfo = uiState.licenseInfo,
                        onClick = onNavigateToLicense
                    )
                }

                // 5. Quick Navigation Cards (Patient Directory, Assigned Tests, Staff)
                item {
                    DashboardQuickNavSection(
                        patientCount = uiState.patientCount,
                        activeStaffCount = uiState.staffSummary?.activeStaff ?: 1,
                        activeTestsCount = uiState.licenseOverview?.reportUsage?.maxReportsPerMonth?.toString() ?: "Catalog",
                        onNavigateToPatients = onNavigateToPatients,
                        onNavigateToTestCatalog = onNavigateToTestCatalog,
                        onNavigateToStaff = onNavigateToStaff
                    )
                }

                // 6. Recent Patients Card
                item {
                    DashboardRecentPatientsCard(
                        patients = uiState.recentPatients,
                        totalCount = uiState.patientCount,
                        onNavigateToPatients = onNavigateToPatients
                    )
                }

                // 7. Recent Laboratory Activity Feed
                item {
                    RecentActivityCard(activities = uiState.recentActivity)
                }

                // 8. Worklist Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Pending Worklist",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (uiState.pendingCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = "${uiState.pendingCount} to enter",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        TextButton(onClick = onNavigateToReports) {
                            Text("View All →", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 9. Worklist Items
                if (uiState.isLoading && uiState.recentDrafts.isEmpty()) {
                    item {
                        SkeletonWorklistList(count = 3)
                    }
                } else if (uiState.recentDrafts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Up to date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All Worklist Reports Completed!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No draft reports pending parameter entry. Tap '+ New Report' to begin a test panel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentDrafts) { report ->
                        WorklistReportItem(
                            report = report,
                            onClick = { onNavigateToReportEntry(report.refId) }
                        )
                    }
                }

                // 10. Test Category Usage Breakdown
                if (uiState.categoryUsage.isNotEmpty()) {
                    item {
                        CategoryUsageCard(categories = uiState.categoryUsage)
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun DashboardGridStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(125.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (isLoading) {
                    SkeletonBox(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .size(width = 44.dp, height = 20.dp)
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardBrandingStatusCard(
    profile: OrganizationProfileResponse?,
    onClick: () -> Unit
) {
    val logoReady = profile?.logoConfigured == true
    val sigReady = profile?.signatureConfigured == true
    val addressReady = !profile?.addressLine1.isNullOrBlank()
    val detailsReady = true

    val configuredCount = listOf(detailsReady, addressReady, logoReady, sigReady).count { it }
    val allReady = configuredCount == 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Lab Letterhead & Branding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mandatory credentials for doctor-signed reports.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                color = if (allReady) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (allReady) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (allReady) Color(0xFF166534) else Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$configuredCount / 4 Branding Steps Completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (allReady) Color(0xFF166534) else Color(0xFFB45309),
                            maxLines = 1
                        )
                    }
                    Surface(
                        color = if (allReady) Color(0xFF166534) else Color(0xFFB45309),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (allReady) "Ready" else "Action Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandingStatusRow(label = "Lab Details & NABL Info", isReady = true)
                BrandingStatusRow(label = "Physical Lab Address", isReady = addressReady)
                BrandingStatusRow(label = "Laboratory Logo", isReady = logoReady)
                BrandingStatusRow(label = "Doctor Signature Stamp", isReady = sigReady)
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Letterhead & Signature", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BrandingStatusRow(label: String, isReady: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (isReady) Color(0xFF166534) else Color(0xFFD97706),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            color = if (isReady) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isReady) "Configured" else "Pending",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReady) Color(0xFF166534) else Color(0xFFB45309),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun DashboardStaffCapacityCard(
    staffSummary: LabStaffSummaryResponse?,
    licenseInfo: LicenseResponse?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Staff Capacity & Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = licenseInfo?.planName ?: "Starter Plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = licenseInfo?.status ?: "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lab Staff Seats", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                val activeCount = staffSummary?.activeStaff ?: 1
                val maxCount = staffSummary?.maxLabStaff ?: 3
                Text("$activeCount / $maxCount Active", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            val maxC = staffSummary?.maxLabStaff ?: 3
            val activeC = staffSummary?.activeStaff ?: 1
            Text(
                text = "${maxC - activeC} staff seats remaining under current plan tier.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Licence & Upgrade", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardQuickNavSection(
    patientCount: Long,
    activeStaffCount: Long,
    activeTestsCount: String,
    onNavigateToPatients: () -> Unit,
    onNavigateToTestCatalog: () -> Unit,
    onNavigateToStaff: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickNavCard(
            title = "Patient Directory",
            badgeCount = "$patientCount",
            subtitle = "Admit new patients, inspect medical histories, and retrieve past lab results.",
            icon = Icons.Default.PersonAdd,
            iconBgColor = Color(0xFFDCFCE7),
            iconTint = Color(0xFF166534),
            onClick = onNavigateToPatients
        )

        QuickNavCard(
            title = "Assigned Tests & Ranges",
            badgeCount = activeTestsCount,
            subtitle = "Inspect biological reference intervals, test units, and assign laboratory tests.",
            icon = Icons.Default.Biotech,
            iconBgColor = Color(0xFFEDE9FE),
            iconTint = Color(0xFF6D28D9),
            onClick = onNavigateToTestCatalog
        )

        QuickNavCard(
            title = "Lab Staff Members",
            badgeCount = "$activeStaffCount",
            subtitle = "Manage pathologists, technicians, and configure permission roles.",
            icon = Icons.Default.Group,
            iconBgColor = Color(0xFFF3E8FF),
            iconTint = Color(0xFF7E22CE),
            onClick = onNavigateToStaff
        )
    }
}

@Composable
fun QuickNavCard(
    title: String,
    badgeCount: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = badgeCount,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun DashboardRecentPatientsCard(
    patients: List<PatientResponse>,
    totalCount: Long,
    onNavigateToPatients: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Recent Patients",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Newly registered patients admitted for diagnostic testing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onNavigateToPatients) {
                    Text("View All →", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (patients.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No recent patients enrolled yet. Tap 'View All' to register a patient.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    patients.take(3).forEach { patient ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onNavigateToPatients)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = patient.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = listOfNotNull(patient.salutation, patient.name).joinToString(" ").ifBlank { "Patient" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val ageText = if (patient.ageValue != null) "${patient.ageValue} ${patient.ageUnit ?: "Yrs"}" else "Age N/A"
                                        val genderText = patient.gender ?: "N/A"
                                        Text(
                                            text = "${patient.patientCode} • $ageText • $genderText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!patient.createdAt.isNullOrBlank()) {
                                        Text(
                                            text = patient.createdAt.take(10),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enrolled patient records",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$totalCount Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryUsageCard(categories: List<CategoryUsageStatsResponse>) {
    val maxCount = (categories.maxOfOrNull { it.count } ?: 1L).coerceAtLeast(1L)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Test Category Usage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            categories.take(6).forEach { cat ->
                val fraction = (cat.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${cat.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecentActivityCard(activities: List<OrgActivityItemResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Laboratory Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "Real-time",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            activities.take(5).forEach { act ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = act.description.ifBlank { "Activity recorded" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = listOfNotNull(act.userName, act.createdAt ?: act.time)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    )
                }
                if (act != activities.take(5).last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun WorklistReportItem(
    report: ReportResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = report.displayPatientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                val testsList = report.tests.joinToString(", ") { it.testName }
                Text(
                    text = if (testsList.isNotBlank()) testsList else "Clinical Test Panel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (!report.refId.isNullOrBlank()) {
                    Text(
                        text = "Ref: #${report.refId.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Enter Values",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
