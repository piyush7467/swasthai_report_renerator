package com.piyush.swasthaireportgenerator.ui.patients

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
import androidx.compose.ui.text.font.FontWeight
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
            text = { Text("Remove ${patient.name} (${patient.patientCode}) from active patient directory?") },
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
                    Text("Delete")
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
                title = "Patient Profile",
                subtitle = if (patient != null) "UHID: ${patient.patientCode}" else null,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (isOrgAdmin && patient != null) {
                        IconButton(onClick = {
                            viewModel.clearCreateError()
                            showEditDialog = true
                        }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Patient")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Patient",
                                tint = MaterialTheme.colorScheme.error
                            )
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
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Demographics Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initials = patient.name.split(" ")
                                                .mapNotNull { it.firstOrNull()?.toString() }
                                                .take(2)
                                                .joinToString("")
                                            Text(
                                                text = initials.ifBlank { "P" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = listOfNotNull(patient.salutation, patient.name).joinToString(" ").trim(),
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

                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = patient.patientCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!patient.phone.isNullOrBlank()) {
                                        ProfileDetailRow(Icons.Default.Phone, "Phone", patient.phone)
                                    }
                                    if (!patient.email.isNullOrBlank()) {
                                        ProfileDetailRow(Icons.Default.Email, "Email", patient.email)
                                    }
                                    if (!patient.address.isNullOrBlank()) {
                                        ProfileDetailRow(Icons.Default.LocationOn, "Address", patient.address)
                                    }
                                    ProfileDetailRow(
                                        Icons.AutoMirrored.Filled.Assignment,
                                        "Total Reports",
                                        "${patient.totalReports ?: uiState.selectedPatientReports.size} Enrolled"
                                    )
                                    if (!patient.createdAt.isNullOrBlank()) {
                                        ProfileDetailRow(
                                            Icons.Default.CalendarToday,
                                            "Admitted Date",
                                            patient.createdAt.take(10)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Patient Report History Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Diagnostic Report History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "${uiState.selectedPatientReports.size} Reports",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (uiState.selectedPatientReports.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No diagnostic reports generated yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.selectedPatientReports) { report ->
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
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(26.dp)
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
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val testNames = report.tests.joinToString(", ") { it.testName }.ifBlank { "Diagnostic Panel" }
                Text(
                    text = testNames,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ref: #${report.refId.take(8).uppercase()} • ${report.createdAt?.take(10) ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isFinalized) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isFinalized) "FINALIZED" else "DRAFT",
                        color = if (isFinalized) Color(0xFF166534) else Color(0xFFB45309),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
