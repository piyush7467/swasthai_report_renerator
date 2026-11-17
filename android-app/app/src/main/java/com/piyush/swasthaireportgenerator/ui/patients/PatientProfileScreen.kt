package com.piyush.swasthaireportgenerator.ui.patients

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    patientRefId: String,
    viewModel: PatientViewModel,
    onNavigateBack: () -> Unit,
    onCreateReportForPatient: (String) -> Unit,
    onNavigateToReportEntry: (String) -> Unit,
    onNavigateToFinalizedReport: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(patientRefId) {
        viewModel.loadPatientProfile(patientRefId)
    }

    val isOrgAdmin = uiState.userRole.equals("ORG_ADMIN", ignoreCase = true)
    val patient = uiState.selectedPatient

    if (showEditDialog && patient != null) {
        EditPatientDialog(
            patient = patient,
            isSaving = uiState.isCreating,
            serverError = uiState.createError,
            onDismissRequest = {
                viewModel.clearCreateError()
                showEditDialog = false
            },
            onSave = { salutation, name, age, ageUnit, gender, phone, email, address ->
                viewModel.updatePatient(
                    patientRefId = patient.refId,
                    salutation = salutation,
                    name = name,
                    age = age,
                    ageUnit = ageUnit,
                    gender = gender,
                    phone = phone,
                    email = email,
                    address = address
                ) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteConfirm && patient != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Patient Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${patient.name} (${patient.patientCode}) from the directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePatient(patient.refId) {
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Patient Clinical Profile",
                subtitle = if (patient != null) "UHID: ${patient.patientCode}" else null,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (patient != null) {
                        IconButton(onClick = {
                            viewModel.clearCreateError()
                            showEditDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Patient Options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Create Diagnostic Report") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onCreateReportForPatient(patient.refId)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh Profile") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.loadPatientProfile(patientRefId)
                                    }
                                )
                                if (isOrgAdmin) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete Patient", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (patient != null) {
                ExtendedFloatingActionButton(
                    onClick = { onCreateReportForPatient(patient.refId) },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Report", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isProfileLoading && patient == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            patient == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Patient record not found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Gender palette token from ui-ux-pro-max
                val genderLower = patient.gender?.trim()?.lowercase() ?: ""
                val (avatarBg, avatarText) = when {
                    genderLower.startsWith("m") -> Color(0xFF0D9488).copy(alpha = 0.15f) to Color(0xFF14B8A6)
                    genderLower.startsWith("f") -> Color(0xFFE11D48).copy(alpha = 0.15f) to Color(0xFFFB7185)
                    else -> Color(0xFF6366F1).copy(alpha = 0.15f) to Color(0xFF818CF8)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 1. Demographics Summary Hero Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 2-tone Initial Avatar
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        color = avatarBg,
                                        border = BorderStroke(1.dp, avatarText.copy(alpha = 0.4f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initials = patient.name.split(" ")
                                                .mapNotNull { it.firstOrNull()?.toString() }
                                                .take(2)
                                                .joinToString("")
                                            Text(
                                                text = initials.ifBlank { "P" }.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = avatarText
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = listOfNotNull(patient.salutation?.let { "$it." }, patient.name).joinToString(" ").trim(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val ageText = if (patient.ageValue != null) "${patient.ageValue} ${patient.ageUnit?.lowercase() ?: "yrs"}" else "Age N/A"
                                        val genderText = patient.gender ?: "N/A"
                                        Text(
                                            text = "$ageText • $genderText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // UHID Pill
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = patient.patientCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Contact & Metadata Rows
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!patient.phone.isNullOrBlank()) {
                                        ProfileInteractiveRow(
                                            icon = Icons.Default.Phone,
                                            label = "Contact Phone",
                                            value = patient.phone,
                                            isActionable = true,
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.phone}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        )
                                    }
                                    if (!patient.email.isNullOrBlank()) {
                                        ProfileInteractiveRow(
                                            icon = Icons.Default.Email,
                                            label = "Email Address",
                                            value = patient.email,
                                            isActionable = true,
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${patient.email}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        )
                                    }
                                    if (!patient.address.isNullOrBlank()) {
                                        ProfileInteractiveRow(
                                            icon = Icons.Default.LocationOn,
                                            label = "Registered Address",
                                            value = patient.address
                                        )
                                    }
                                    ProfileInteractiveRow(
                                        icon = Icons.AutoMirrored.Filled.Assignment,
                                        label = "Diagnostic Records",
                                        value = "${patient.totalReports ?: uiState.selectedPatientReports.size} Enrolled Reports"
                                    )
                                    if (!patient.createdAt.isNullOrBlank()) {
                                        ProfileInteractiveRow(
                                            icon = Icons.Default.CalendarToday,
                                            label = "Registration Date",
                                            value = patient.createdAt.take(10)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                // Quick Action Buttons Dock
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onCreateReportForPatient(patient.refId) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New Test", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.clearCreateError()
                                            showEditDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit Details")
                                    }
                                }
                            }
                        }
                    }

                    // 2. Patient Report History Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Biotech,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Diagnostic Report History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "${uiState.selectedPatientReports.size} ${if (uiState.selectedPatientReports.size == 1) "Report" else "Reports"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Empty State or List
                    if (uiState.selectedPatientReports.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Science,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "No Diagnostic Reports Yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Assign test panels (CBC, Lipid, LFT) to generate certified laboratory reports.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = { onCreateReportForPatient(patient.refId) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Report", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.selectedPatientReports,
                            key = { it.refId }
                        ) { report ->
                            PatientReportHistoryCard(
                                report = report,
                                onClick = {
                                    if (report.status.equals("FINALIZED", ignoreCase = true)) {
                                        onNavigateToFinalizedReport(report.refId)
                                    } else {
                                        onNavigateToReportEntry(report.refId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInteractiveRow(
    icon: ImageVector,
    label: String,
    value: String,
    isActionable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActionable && onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                        .padding(vertical = 2.dp)
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isActionable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isActionable) {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PatientReportHistoryCard(
    report: ReportResponse,
    onClick: () -> Unit
) {
    val isFinalized = report.status.equals("FINALIZED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFinalized) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFinalized) Icons.Default.CheckCircle else Icons.Default.EditNote,
                            contentDescription = null,
                            tint = if (isFinalized) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val testNames = report.tests.joinToString(", ") { it.testName }.ifBlank { "Diagnostic Panel" }
                    Text(
                        text = testNames,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ref: #${report.refId.take(8).uppercase()} • ${report.createdAt?.take(10) ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isFinalized) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isFinalized) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isFinalized) "FINALIZED" else "DRAFT",
                        color = if (isFinalized) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Report",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EditPatientDialog(
    patient: PatientResponse,
    isSaving: Boolean,
    serverError: String?,
    onDismissRequest: () -> Unit,
    onSave: (
        salutation: String,
        name: String,
        age: Int,
        ageUnit: String,
        gender: String,
        phone: String?,
        email: String?,
        address: String?
    ) -> Unit
) {
    AddPatientDialogInternal(
        dialogTitle = "Edit Patient Profile",
        confirmLabel = "Save Changes",
        initialSalutation = patient.salutation ?: "MR",
        initialName = patient.name,
        initialAge = patient.ageValue?.toString() ?: "",
        initialAgeUnit = patient.ageUnit ?: "YEARS",
        initialGender = patient.gender ?: "MALE",
        initialPhone = patient.phone ?: "",
        initialEmail = patient.email ?: "",
        initialAddress = patient.address ?: "",
        onDismissRequest = onDismissRequest,
        onSavePatient = onSave,
        isSaving = isSaving,
        serverError = serverError
    )
}
