package com.piyush.swasthaireportgenerator.ui.license

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.AvailablePlanResponse
import com.piyush.swasthaireportgenerator.data.model.LicenseResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationLicenseOverviewResponse
import com.piyush.swasthaireportgenerator.data.model.PlanUpgradeRequestResponse
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    viewModel: LicenseViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var planSelectedForUpgrade by remember { mutableStateOf<AvailablePlanResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    if (planSelectedForUpgrade != null) {
        RequestUpgradeDialog(
            targetPlan = planSelectedForUpgrade!!,
            defaultName = uiState.userName,
            defaultEmail = uiState.userEmail,
            isSubmitting = uiState.isSubmitting,
            serverError = uiState.submitError,
            onDismissRequest = {
                viewModel.clearSubmitError()
                planSelectedForUpgrade = null
            },
            onSubmit = { name, email, phone, reason ->
                viewModel.submitUpgradeRequest(
                    requestedPlanRefId = planSelectedForUpgrade!!.refId,
                    contactName = name,
                    contactEmail = email,
                    contactPhone = phone,
                    reason = reason
                ) {
                    planSelectedForUpgrade = null
                }
            }
        )
    }

    val isOrgAdmin = uiState.userRole.equals("ORG_ADMIN", ignoreCase = true)

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Plans & Subscription",
                subtitle = "Manage organization plan & resource limits",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.basicLicense == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Header Row & Action Toolbar (Fixed title size & padding to prevent overlap)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "License & Subscription",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF166534),
                                            modifier = Modifier.size(12.dp).padding(start = 2.dp)
                                        )
                                    }
                                }

                                if (isOrgAdmin && uiState.availablePlans.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val target = uiState.availablePlans.firstOrNull { it.upgrade } ?: uiState.availablePlans.firstOrNull()
                                            if (target != null) {
                                                viewModel.clearSubmitError()
                                                planSelectedForUpgrade = target
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Upgrade", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            Text(
                                text = "Manage your organization subscription plan, resource utilization, and team capacity limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. Active License Banner
                    val activeLicense = uiState.overview?.license ?: uiState.basicLicense
                    if (activeLicense != null) {
                        item {
                            ActiveLicenseCard(
                                license = activeLicense,
                                daysRemaining = uiState.overview?.daysRemaining,
                                onRequestUpgrade = {
                                    val target = uiState.availablePlans.firstOrNull { it.upgrade } ?: uiState.availablePlans.firstOrNull()
                                    if (target != null) {
                                        viewModel.clearSubmitError()
                                        planSelectedForUpgrade = target
                                    }
                                }
                            )
                        }
                    }

                    // 2. Resource Utilization (ORG_ADMIN overview)
                    if (isOrgAdmin && uiState.overview != null) {
                        item {
                            UsageUtilizationCard(overview = uiState.overview!!)
                        }
                    }

                    // 3. Available Plans Catalog (ORG_ADMIN)
                    if (isOrgAdmin && uiState.availablePlans.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Subscription Plan Specifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Capabilities and provisions enabled under the active tier.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(items = uiState.availablePlans, key = { it.refId }) { plan ->
                            AvailablePlanCard(
                                plan = plan,
                                onRequestUpgrade = {
                                    viewModel.clearSubmitError()
                                    planSelectedForUpgrade = plan
                                }
                            )
                        }
                    }

                    // 4. Upgrade Requests History (ORG_ADMIN)
                    if (isOrgAdmin) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Upgrade Request History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Track communication and status updates on your organization's subscription requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.upgradeRequests.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No upgrade requests found",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "When your organization requests a plan upgrade, it will appear here with real-time status tracking.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(items = uiState.upgradeRequests, key = { it.refId }) { req ->
                                UpgradeRequestItemCard(
                                    request = req,
                                    onCancel = { viewModel.cancelRequest(req.refId) }
                                )
                            }
                        }
                    } else {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Plan upgrades and staff capacity changes are managed by your Organization Administrator.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveLicenseCard(
    license: LicenseResponse,
    daysRemaining: Long?,
    onRequestUpgrade: () -> Unit
) {
    val isUsable = license.currentlyUsable
    val activeMint = Color(0xFF10B981)
    val inactiveRed = Color(0xFFEF4444)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        shape = RoundedCornerShape(12.dp),
                        color = activeMint.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, activeMint.copy(alpha = 0.3f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = activeMint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = license.planName ?: "Starter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = if (isUsable) activeMint.copy(alpha = 0.15f) else inactiveRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isUsable) activeMint.copy(alpha = 0.3f) else inactiveRed.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = license.status ?: if (isUsable) "ACTIVE" else "INACTIVE",
                                    color = if (isUsable) activeMint else inactiveRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "License: ${license.refId.take(16).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onRequestUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // 3 Metadata Boxes (Start Date, Expiry Date, Remaining Validity)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("START DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = license.startedAt?.take(10) ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("EXPIRY DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = license.expiresAt?.take(10) ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("REMAINING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${daysRemaining ?: 0} days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsageUtilizationCard(overview: OrganizationLicenseOverviewResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Resource Utilization & Capacity Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time metrics monitored against your organization's subscription plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            overview.staffUsage?.let { staff ->
                val max = staff.maxLabStaff.coerceAtLeast(1)
                val healthColor = if (staff.limitReached) Color(0xFFEF4444) else Color(0xFF10B981)
                UsageMetricCard(
                    title = "Lab Staff License Seats",
                    subtitle = "Active technicians and pathologists",
                    valueText = "${staff.activeStaff} of ${staff.maxLabStaff} seats allocated",
                    badgeText = if (staff.limitReached) "Limit Reached" else "Healthy Usage",
                    badgeBg = healthColor.copy(alpha = 0.15f),
                    badgeFg = healthColor,
                    progress = (staff.activeStaff.toFloat() / max.toFloat()).coerceIn(0f, 1f),
                    footerText = "${(staff.maxLabStaff - staff.activeStaff).coerceAtLeast(0)} seats available",
                    isAlert = staff.limitReached
                )
            }

            overview.reportUsage?.let { rep ->
                val monthMax = rep.maxReportsPerMonth.coerceAtLeast(1)
                val healthColor = if (rep.monthlyLimitReached) Color(0xFFEF4444) else Color(0xFF10B981)
                UsageMetricCard(
                    title = "Monthly Report Generation",
                    subtitle = "Reports drafted and finalized this month",
                    valueText = "${rep.monthlyReportsCreated} of ${rep.maxReportsPerMonth} monthly reports used",
                    badgeText = if (rep.monthlyLimitReached) "Limit Reached" else "Healthy Usage",
                    badgeBg = healthColor.copy(alpha = 0.15f),
                    badgeFg = healthColor,
                    progress = (rep.monthlyReportsCreated.toFloat() / monthMax.toFloat()).coerceIn(0f, 1f),
                    footerText = "Today: ${rep.dailyReportsCreated} / ${rep.maxReportsPerDay} reports • Resets on 1st of month",
                    isAlert = rep.monthlyLimitReached
                )
            }

            // Registered Patients & Lifetime Reports Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("REGISTERED PATIENTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${overview.patientUsage?.totalPatients ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("records in system", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LIFETIME REPORTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${overview.reportUsage?.totalReports ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("diagnostic records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun UsageMetricCard(
    title: String,
    subtitle: String,
    valueText: String,
    badgeText: String,
    badgeBg: Color,
    badgeFg: Color,
    progress: Float,
    footerText: String,
    isAlert: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (isAlert) MaterialTheme.colorScheme.error else Color(0xFF0D9488)
            )

            Text(
                text = footerText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AvailablePlanCard(
    plan: AvailablePlanResponse,
    onRequestUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${plan.currency ?: "INR"} ${plan.annualPrice?.toInt() ?: 0} / year",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (plan.currentPlan) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CURRENT",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(if (plan.upgrade) "Upgrade" else "Select", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!plan.description.isNullOrBlank()) {
                Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Up to ${plan.maxLabStaff} active users • ${plan.maxReportsPerMonth ?: "∞"} monthly reports • ${plan.maxReportsPerDay ?: "∞"} daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun UpgradeRequestItemCard(
    request: PlanUpgradeRequestResponse,
    onCancel: () -> Unit
) {
    val status = request.status.uppercase()
    val isPending = status == "PENDING"
    val statusColor = when (status) {
        "APPROVED" -> Color(0xFF10B981)
        "REJECTED", "CANCELLED" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${request.currentPlanName ?: "Current"} → ${request.requestedPlanName ?: "Target Plan"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (!request.reason.isNullOrBlank()) {
                Text("Reason: ${request.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!request.rejectionReason.isNullOrBlank()) {
                Text("Note: ${request.rejectionReason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Submitted: ${request.createdAt?.take(10) ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (isPending) {
                    TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Cancel Request", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestUpgradeDialog(
    targetPlan: AvailablePlanResponse,
    defaultName: String,
    defaultEmail: String,
    isSubmitting: Boolean,
    serverError: String?,
    onDismissRequest: () -> Unit,
    onSubmit: (name: String, email: String, phone: String?, reason: String?) -> Unit
) {
    var contactName by remember { mutableStateOf(defaultName) }
    var contactEmail by remember { mutableStateOf(defaultEmail) }
    var contactPhone by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val activeError = localError ?: serverError

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismissRequest() },
        shape = RoundedCornerShape(16.dp),
        title = { Text("Request Plan Upgrade", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Target: ${targetPlan.name} (${targetPlan.currency ?: "INR"} ${targetPlan.annualPrice?.toInt() ?: 0}/yr)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (!activeError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Contact Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Contact Phone (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Upgrade (Optional)") },
                    placeholder = { Text("e.g. Adding more staff members this month") },
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contactName.isBlank() || contactEmail.isBlank()) {
                        localError = "Name and email are required"
                        return@Button
                    }
                    localError = null
                    onSubmit(contactName, contactEmail, contactPhone.ifBlank { null }, reason.ifBlank { null })
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Request")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}
