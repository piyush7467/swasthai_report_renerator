package com.piyush.swasthaireportgenerator.ui.staff

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.LabStaffDetailsResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffResponse
import com.piyush.swasthaireportgenerator.data.model.LabStaffSummaryResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    viewModel: StaffViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffPendingDelete by remember { mutableStateOf<LabStaffResponse?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    if (showAddStaffDialog) {
        AddLabStaffDialog(
            isSaving = uiState.isMutating,
            serverError = uiState.createError,
            onDismissRequest = {
                viewModel.clearCreateError()
                showAddStaffDialog = false
            },
            onCreateStaff = { name, email, pass, confirm ->
                viewModel.createStaff(name, email, pass, confirm) {
                    showAddStaffDialog = false
                }
            }
        )
    }

    if (uiState.selectedDetails != null) {
        StaffDetailsDialog(
            details = uiState.selectedDetails!!,
            onDismissRequest = { viewModel.clearSelectedDetails() }
        )
    }

    if (staffPendingDelete != null) {
        val target = staffPendingDelete!!
        AlertDialog(
            onDismissRequest = { staffPendingDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Staff Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Remove ${target.name} (${target.email}) from laboratory staff access?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStaff(target.refId) {
                            staffPendingDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { staffPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isLimitReached = uiState.summary?.limitReached ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Lab Staff Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage technician accounts & seat quota",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Staff Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Staff List") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.refreshAll()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Seat Quota") },
                                leadingIcon = { Icon(Icons.Default.CardMembership, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToPlans()
                                }
                            )
                            if (!isLimitReached) {
                                DropdownMenuItem(
                                    text = { Text("Add New Staff") },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.clearCreateError()
                                        showAddStaffDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isLimitReached) {
                        onNavigateToPlans()
                    } else {
                        viewModel.clearCreateError()
                        showAddStaffDialog = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isLimitReached) Icons.Default.Upgrade else Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                },
                text = { Text(if (isLimitReached) "Upgrade Quota" else "Add Staff") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
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

            // 1. License Seat Quota Summary Card
            if (uiState.summary != null) {
                item {
                    StaffSummaryQuotaCard(
                        summary = uiState.summary!!,
                        onUpgradeClick = onNavigateToPlans
                    )
                }
            }

            // 2. Search + Status Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.loadStaffList(search = it) },
                        placeholder = { Text("Search staff by name or email...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.loadStaffList(search = "") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "All", "ACTIVE" to "Active", "INACTIVE" to "Inactive").forEach { (statusKey, label) ->
                            FilterChip(
                                selected = uiState.selectedStatusFilter == statusKey,
                                onClick = { viewModel.loadStaffList(status = statusKey) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 3. Staff List
            if (uiState.isLoading && uiState.staffList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.staffList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text("No Lab Staff Found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add laboratory technicians to allow specimen intake and report entry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.staffList,
                    key = { it.refId }
                ) { staff ->
                    StaffMemberCard(
                        staff = staff,
                        onViewDetails = { viewModel.loadStaffDetails(staff.refId) },
                        onToggleStatus = { viewModel.toggleStaffStatus(staff) },
                        onDelete = { staffPendingDelete = staff }
                    )
                }
            }
        }
    }
}
}

@Composable
fun StaffSummaryQuotaCard(
    summary: LabStaffSummaryResponse,
    onUpgradeClick: () -> Unit
) {
    val maxSlots = summary.maxLabStaff.coerceAtLeast(1)
    val ratio = (summary.activeStaff.toFloat() / maxSlots.toFloat()).coerceIn(0f, 1f)

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
                    Text(
                        text = summary.planName ?: "Active License Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${summary.activeStaff} of ${summary.maxLabStaff} active staff slots used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (summary.limitReached) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(
                        1.dp,
                        if (summary.limitReached) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        else Color(0xFF10B981).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (summary.limitReached) "Quota Full" else "${summary.remainingSlots} slots left",
                        color = if (summary.limitReached) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (summary.limitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            if (summary.limitReached) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upgrade plan to add more staff.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onUpgradeClick) {
                        Text("Upgrade Plan →", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffMemberCard(
    staff: LabStaffResponse,
    onViewDetails: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = staff.status.equals("ACTIVE", ignoreCase = true)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = staff.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                        Text(
                            text = initials.ifBlank { "S" }.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = staff.name,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = staff.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isActive) "ACTIVE" else "INACTIVE",
                        color = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Staff Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Activity Details") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onViewDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isActive) "Deactivate Account" else "Activate Account") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isActive) Color(0xFFD97706) else Color(0xFF0D9488)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleStatus()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Staff Account", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${staff.reportsCreated ?: 0} created",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFCCFBF1),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${staff.reportsFinalized ?: 0} finalized",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "View details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddLabStaffDialog(
    isSaving: Boolean,
    serverError: String?,
    onDismissRequest: () -> Unit,
    onCreateStaff: (name: String, email: String, password: String, confirmPassword: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val activeError = localError ?: serverError

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        shape = RoundedCornerShape(24.dp),
        title = { Text("Add Lab Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!activeError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 8 chars) *") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Full name is required."
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                        localError = "Please enter a valid email address."
                        return@Button
                    }
                    if (password.length < 8) {
                        localError = "Password must be at least 8 characters."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        localError = "Passwords do not match."
                        return@Button
                    }
                    localError = null
                    onCreateStaff(name, email, password, confirmPassword)
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Create Account")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StaffDetailsDialog(
    details: LabStaffDetailsResponse,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = { Text(details.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Email: ${details.email}", style = MaterialTheme.typography.bodySmall)
                Text("Role: ${details.role ?: "LAB_STAFF"}", style = MaterialTheme.typography.bodySmall)
                Text("Status: ${details.status}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Text("Reports Created: ${details.reportsCreated}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Reports Finalized: ${details.reportsFinalized}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Last Login: ${details.lastLoginAt?.take(19)?.replace("T", " ") ?: "Never"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
