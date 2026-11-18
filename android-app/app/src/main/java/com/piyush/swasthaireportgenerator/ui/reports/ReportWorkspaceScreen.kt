package com.piyush.swasthaireportgenerator.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.R
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.OrganizationTestResponse
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.data.model.ReportParameterResultResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar
import com.piyush.swasthaireportgenerator.ui.patients.AddPatientDialog
import com.piyush.swasthaireportgenerator.ui.patients.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportWorkspaceScreen(
    initialReportRefId: String? = null,
    initialPatientRefId: String? = null,
    reportViewModel: ReportViewModel,
    patientViewModel: PatientViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFinalized: (finalizedRefId: String) -> Unit
) {
    val entryState by reportViewModel.entryState.collectAsState()
    val patientState by patientViewModel.uiState.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }
    var selectedPatient by remember { mutableStateOf<PatientResponse?>(null) }
    var selectedDoctor by remember { mutableStateOf("") }
    var clinicalNotes by remember { mutableStateOf("") }
    var selectedTestRefIds by remember { mutableStateOf(setOf<String>()) }
    var isCreatingDraft by remember { mutableStateOf(false) }
    var showAddPatientDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        reportViewModel.loadWorkspaceContext()
        patientViewModel.loadPatients()
    }

    LaunchedEffect(initialReportRefId) {
        if (!initialReportRefId.isNullOrBlank()) {
            reportViewModel.loadReport(initialReportRefId)
            currentStep = 4
        }
    }

    LaunchedEffect(initialPatientRefId, patientState.patients) {
        if (!initialPatientRefId.isNullOrBlank() && selectedPatient == null) {
            val found = patientState.patients.find { it.refId == initialPatientRefId }
            if (found != null) {
                selectedPatient = found
                if (currentStep == 1 && initialReportRefId.isNullOrBlank()) {
                    currentStep = 2
                }
            }
        }
    }

    val activeReport = entryState.report

    LaunchedEffect(activeReport?.tests) {
        val existingIds = activeReport?.tests?.mapNotNull { it.testRefId }?.toSet()
        if (!existingIds.isNullOrEmpty()) {
            selectedTestRefIds = selectedTestRefIds + existingIds
        }
    }

    if (showAddPatientDialog) {
        AddPatientDialog(
            isSaving = patientState.isCreating,
            serverError = patientState.createError,
            onDismissRequest = {
                patientViewModel.clearCreateError()
                showAddPatientDialog = false
            },
            onSavePatient = { sal, name, age, unit, gender, phone, email, address ->
                patientViewModel.createPatient(
                    salutation = sal,
                    name = name,
                    age = age,
                    ageUnit = unit,
                    gender = gender,
                    phone = phone,
                    email = email,
                    address = address,
                    onSuccess = { newPatient ->
                        selectedPatient = newPatient
                        showAddPatientDialog = false
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = when (currentStep) {
                    1 -> "Select Patient"
                    2 -> "Assign Doctor & Notes"
                    3 -> "Select Diagnostic Tests"
                    4 -> "Enter Parameter Values"
                    5 -> "Final Review & Sign"
                    else -> "Clinical Report Workspace"
                },
                subtitle = activeReport?.let { "Draft Ref: #${it.refId.take(8).uppercase()}" }
                    ?: selectedPatient?.let { "Patient: ${it.name}" },
                canNavigateBack = true,
                onNavigateBack = {
                    if (currentStep > 1) {
                        if (currentStep == 4) {
                            reportViewModel.silentAutoSaveParameters()
                        }
                        currentStep--
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (currentStep == 4) {
                            DropdownMenuItem(
                                text = { Text("Recalculate Values") },
                                leadingIcon = {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    activeReport?.refId?.let { ref ->
                                        reportViewModel.saveAndRecalculate(ref)
                                    }
                                }
                            )
                        }
                        if (activeReport != null) {
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    reportViewModel.loadReport(activeReport.refId)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Exit to Reports") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showMenu = false
                                onNavigateBack()
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 5-STEP STEPPER BAR (Horizontally scrollable, compact & auto-scrolling)
            WorkspaceStepperHeader(
                currentStep = currentStep,
                onStepClick = { step ->
                    if (step < currentStep || (step == 4 && activeReport != null)) {
                        if (currentStep == 4 && step < 4) {
                            reportViewModel.silentAutoSaveParameters()
                        }
                        currentStep = step
                    }
                }
            )

            // STEP CONTENT SWITCHER
            when (currentStep) {
                1 -> Step1PatientSelection(
                    patients = patientState.patients,
                    isPatientsLoading = patientState.isLoading,
                    patientsError = patientState.errorMessage,
                    selectedPatient = selectedPatient,
                    readOnly = activeReport != null,
                    onRefreshPatients = { patientViewModel.loadPatients() },
                    onSelectPatient = { pat ->
                        selectedPatient = if (selectedPatient?.refId == pat?.refId) null else pat
                    },
                    onOpenAddPatient = { showAddPatientDialog = true },
                    onNext = { currentStep = 2 }
                )

                2 -> Step2DoctorSelection(
                    organizationProfile = entryState.organizationProfile,
                    signatoryOptions = entryState.signatoryOptions,
                    selectedDoctor = selectedDoctor.ifBlank { entryState.signatoryOptions.firstOrNull() ?: "" },
                    onDoctorChange = { doc ->
                        selectedDoctor = if (selectedDoctor == doc) "" else doc
                    },
                    clinicalNotes = clinicalNotes,
                    onNotesChange = { clinicalNotes = it },
                    onBack = { currentStep = 1 },
                    onNext = { currentStep = 3 }
                )

                3 -> Step3TestSelection(
                    availableTests = entryState.availableTests,
                    isTestsLoading = entryState.isTestsLoading,
                    testsErrorMessage = entryState.testsErrorMessage,
                    alreadyAddedTestRefIds = activeReport?.tests?.mapNotNull { it.testRefId }?.toSet() ?: emptySet(),
                    selectedTestRefIds = selectedTestRefIds,
                    errorMessage = entryState.errorMessage,
                    isCreating = isCreatingDraft || entryState.isLoading,
                    onRetryLoadTests = { reportViewModel.loadOrganizationTests() },
                    onToggleTest = { testRefId ->
                        selectedTestRefIds = if (selectedTestRefIds.contains(testRefId)) {
                            selectedTestRefIds - testRefId
                        } else {
                            selectedTestRefIds + testRefId
                        }
                    },
                    onBack = { currentStep = 2 },
                    onProceedToResults = {
                        val pat = selectedPatient
                        if (activeReport == null && pat != null) {
                            isCreatingDraft = true
                            reportViewModel.createReportWithTests(
                                patientRefId = pat.refId,
                                testRefIds = selectedTestRefIds.toList(),
                                onSuccess = {
                                    isCreatingDraft = false
                                    currentStep = 4
                                }
                            )
                        } else if (activeReport != null) {
                            val newTests = selectedTestRefIds - (activeReport.tests.mapNotNull { it.testRefId }.toSet())
                            if (newTests.isNotEmpty()) {
                                isCreatingDraft = true
                                reportViewModel.addTestsToExistingReport(
                                    reportRefId = activeReport.refId,
                                    newTestRefIds = newTests.toList(),
                                    onSuccess = {
                                        isCreatingDraft = false
                                        currentStep = 4
                                    }
                                )
                            } else {
                                currentStep = 4
                            }
                        }
                    }
                )

                4 -> Step4ParameterResults(
                    entryState = entryState,
                    onParameterChange = { paramRefId, valStr -> reportViewModel.onParameterChange(paramRefId, valStr) },
                    onRemoveTest = { testRefId -> activeReport?.let { reportViewModel.removeTest(it.refId, testRefId) } },
                    onAddTest = { testRefId ->
                        activeReport?.let { rep ->
                            reportViewModel.addTestsToExistingReport(rep.refId, listOf(testRefId)) {}
                        }
                    },
                    onRecalculate = { activeReport?.let { reportViewModel.saveAndRecalculate(it.refId) } },
                    onBack = {
                        reportViewModel.silentAutoSaveParameters()
                        currentStep = 3
                    },
                    onNext = {
                        reportViewModel.saveAndProceedToReview {
                            currentStep = 5
                        }
                    }
                )

                5 -> Step5PreFinalizationReview(
                    report = entryState.report,
                    includeLetterheadHeader = entryState.report?.includeOrganizationHeader ?: true,
                    errorMessage = entryState.errorMessage,
                    onToggleHeader = { include -> activeReport?.let { reportViewModel.updateHeaderOption(it.refId, include) } },
                    isFinalizing = entryState.isFinalizing,
                    onBack = { currentStep = 4 },
                    onFinalizeClick = {
                        activeReport?.let { rep ->
                            reportViewModel.finalizeReport(rep.refId) {
                                onNavigateToFinalized(rep.refId)
                            }
                        }
                    }
                )
            }
        }
    }
}

// =========================================================================
// 5-STEP WORKSPACE STEPPER HEADER (Horizontally Scrollable & Auto-Scrolling)
// =========================================================================
@Composable
fun WorkspaceStepperHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf("Patient", "Doctor", "Tests", "Results", "Review")
    val listState = rememberLazyListState()

    LaunchedEffect(currentStep) {
        listState.animateScrollToItem(maxOf(0, currentStep - 1))
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(steps) { index, title ->
            val stepNumber = index + 1
            val isActive = currentStep == stepNumber
            val isCompleted = currentStep > stepNumber

            Surface(
                color = when {
                    isActive -> Color(0xFF0D9488) // Primary Teal
                    isCompleted -> Color(0xFF10B981).copy(alpha = 0.15f) // Mint tint
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
                shape = RoundedCornerShape(100.dp),
                border = when {
                    isActive -> BorderStroke(1.dp, Color(0xFF14B8A6))
                    isCompleted -> BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onStepClick(stepNumber) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$stepNumber",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isActive -> Color.White
                            isCompleted -> Color(0xFF047857)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (index < steps.size - 1) {
                Text(
                    text = "›",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =========================================================================
// STEP 1: PATIENT SELECTION
// =========================================================================
@Composable
fun Step1PatientSelection(
    patients: List<PatientResponse>,
    isPatientsLoading: Boolean,
    patientsError: String?,
    selectedPatient: PatientResponse?,
    readOnly: Boolean,
    onRefreshPatients: () -> Unit,
    onSelectPatient: (PatientResponse?) -> Unit,
    onOpenAddPatient: () -> Unit,
    onNext: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = patients.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.patientCode.contains(searchQuery, ignoreCase = true) ||
            (it.phone?.contains(searchQuery) == true)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Row with "+ New Patient" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Select Patient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selectedPatient != null) "Patient confirmed for report" else "Choose existing or register a new patient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            if (!readOnly) {
                Button(
                    onClick = onOpenAddPatient,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "New Patient",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Patient",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Selected Patient Confirmation Card
        if (selectedPatient != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color(0xFF0D9488)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initials = selectedPatient.name.trim().split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .map { it.first().uppercase() }
                                .joinToString("")
                                .ifBlank { "P" }
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${selectedPatient.salutation ?: ""} ${selectedPatient.name}".trim(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Surface(
                                color = Color(0xFF0D9488),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "SELECTED",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UHID: ${selectedPatient.patientCode} • ${selectedPatient.ageValue ?: 0} ${selectedPatient.ageUnit ?: "Yrs"} • ${selectedPatient.gender ?: ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        if (!selectedPatient.phone.isNullOrBlank()) {
                            Text(
                                text = "📞 ${selectedPatient.phone}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (!readOnly) {
                        OutlinedButton(
                            onClick = { onSelectPatient(null) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D9488)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D9488))
                        }
                    }
                }
            }
        }

        if (!readOnly) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, UHID, or phone...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    } else {
                        IconButton(onClick = onRefreshPatients) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Patients")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!patientsError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = patientsError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefreshPatients) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isPatientsLoading && patients.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0D9488))
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = Color(0xFF0D9488).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isBlank()) "No registered patients yet" else "No patients match \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Register this patient now without leaving report creation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onOpenAddPatient,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Register New Patient", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filtered, key = { it.refId }) { patient ->
                        val isSelected = selectedPatient?.refId == patient.refId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectPatient(patient) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initials = patient.name.trim().split(" ")
                                                .filter { it.isNotBlank() }
                                                .take(2)
                                                .map { it.first().uppercase() }
                                                .joinToString("")
                                                .ifBlank { "P" }
                                            Text(
                                                text = initials,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "${patient.salutation ?: ""} ${patient.name}".trim(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "UHID: ${patient.patientCode} • ${patient.ageValue ?: 0} ${patient.ageUnit ?: "Yrs"} • ${patient.gender ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                        if (!patient.phone.isNullOrBlank()) {
                                            Text(
                                                text = "📞 ${patient.phone}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectPatient(patient) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF0D9488),
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = onNext,
            enabled = selectedPatient != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Proceed to Referring Doctor →", fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// STEP 2: REFERRING DOCTOR / CLINICAL ASSIGNMENT
// =========================================================================
@Composable
fun Step2DoctorSelection(
    organizationProfile: OrganizationProfileResponse?,
    signatoryOptions: List<String>,
    selectedDoctor: String,
    onDoctorChange: (String) -> Unit,
    clinicalNotes: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (organizationProfile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = organizationProfile.organizationName ?: "Diagnostic Facility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (organizationProfile.displayAddress.isNotBlank()) {
                            Text(
                                text = organizationProfile.displayAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!organizationProfile.signatureOwnerName.isNullOrBlank()) {
                            Text(
                                text = "Signatory on file: ${organizationProfile.signatureOwnerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Assign Reporting Clinician",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Signatory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        if (signatoryOptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(signatoryOptions) { doc ->
                    val isSelected = selectedDoctor == doc
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onDoctorChange(doc) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Authorized Reporting Clinician",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onDoctorChange(doc) }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = clinicalNotes,
            onValueChange = onNotesChange,
            label = { Text("Clinical Remarks / Sample Notes (Optional)") },
            placeholder = { Text("e.g. Fasting sample, 12h post-prandial, EDTA vial") },
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            ) {
                Text("← Back")
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f).defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Proceed to Tests →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// STEP 3: SELECT DIAGNOSTIC TEST PANELS (FROM ORGANIZATION TEST CATALOG)
// =========================================================================
@Composable
fun Step3TestSelection(
    availableTests: List<OrganizationTestResponse>,
    isTestsLoading: Boolean,
    testsErrorMessage: String?,
    alreadyAddedTestRefIds: Set<String>,
    selectedTestRefIds: Set<String>,
    errorMessage: String?,
    isCreating: Boolean,
    onRetryLoadTests: () -> Unit,
    onToggleTest: (String) -> Unit,
    onBack: () -> Unit,
    onProceedToResults: () -> Unit
) {
    var searchTerm by remember { mutableStateOf("") }
    val filteredTests = availableTests.filter {
        searchTerm.isBlank() ||
            it.testName.contains(searchTerm, ignoreCase = true) ||
            it.testCode.contains(searchTerm, ignoreCase = true)
    }

    val totalEffectiveSelected = selectedTestRefIds.size + alreadyAddedTestRefIds.size

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Select Test Panels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (totalEffectiveSelected > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$totalEffectiveSelected Selected",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (totalEffectiveSelected > 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchTerm,
            onValueChange = { searchTerm = it },
            placeholder = { Text("Search tests (e.g. CBC, Lipid, LFT, KFT)...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchTerm.isNotBlank()) {
                    IconButton(onClick = { searchTerm = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isTestsLoading && availableTests.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !testsErrorMessage.isNullOrBlank() && availableTests.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = testsErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onRetryLoadTests) {
                            Text("Retry")
                        }
                    }
                }
                filteredTests.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchTerm.isNotBlank()) "No tests matching \"$searchTerm\"" else "No active lab tests assigned to your organization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items = filteredTests, key = { it.testRefId }) { test ->
                            val isAlreadyAdded = alreadyAddedTestRefIds.contains(test.testRefId)
                            val isChecked = isAlreadyAdded || selectedTestRefIds.contains(test.testRefId)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isAlreadyAdded) { onToggleTest(test.testRefId) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = if (isChecked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 2.dp else 1.dp)
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
                                            modifier = Modifier.size(42.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Biotech,
                                                    contentDescription = null,
                                                    tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = test.testName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                ) {
                                                    Text(
                                                        text = test.testCode,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isAlreadyAdded) "Already added to report" else (test.testType ?: "Standard Diagnostic Panel"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isAlreadyAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        enabled = !isAlreadyAdded,
                                        onCheckedChange = { onToggleTest(test.testRefId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            ) {
                Text("← Back")
            }
            Button(
                onClick = onProceedToResults,
                enabled = totalEffectiveSelected > 0 && !isCreating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.8f).defaultMinSize(minHeight = 48.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (alreadyAddedTestRefIds.isEmpty()) "Create Draft & Enter Results →" else "Continue to Results →",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =========================================================================
// STEP 4: PARAMETER RESULTS ENTRY & RECALCULATION (Mobile-Optimized!)
// =========================================================================
@Composable
fun Step4ParameterResults(
    entryState: ReportEntryUiState,
    onParameterChange: (String, String) -> Unit,
    onRemoveTest: (String) -> Unit,
    onAddTest: (String) -> Unit,
    onRecalculate: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val report = entryState.report
    val allParameters = remember(report) { report?.tests?.flatMap { it.parameters } ?: emptyList() }
    var testPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeParameterId by remember { mutableStateOf<String?>(null) }
    var validationErrorDialog by remember { mutableStateOf<String?>(null) }
    var serverErrorDialog by remember { mutableStateOf<String?>(null) }
    var showAddTestDialog by remember { mutableStateOf(false) }
    var testFilterQuery by remember { mutableStateOf("") }

    LaunchedEffect(entryState.errorMessage) {
        if (!entryState.errorMessage.isNullOrBlank()) {
            serverErrorDialog = entryState.errorMessage
        }
    }

    if (showAddTestDialog && report != null) {
        val existingCodes = remember(report.tests) { report.tests.map { it.testCode.uppercase() }.toSet() }
        val existingIds = remember(report.tests) { report.tests.mapNotNull { it.testRefId }.toSet() }
        val availableToAdd = remember(entryState.availableTests, existingCodes, existingIds) {
            entryState.availableTests.filter { it.refId !in existingIds && it.testCode.uppercase() !in existingCodes }
        }
        val filteredTests = remember(availableToAdd, testFilterQuery) {
            if (testFilterQuery.isBlank()) availableToAdd
            else availableToAdd.filter {
                it.testName.contains(testFilterQuery, ignoreCase = true) ||
                it.testCode.contains(testFilterQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddTestDialog = false
                testFilterQuery = ""
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF0D9488))
                    Text("Add Test Panel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = testFilterQuery,
                        onValueChange = { testFilterQuery = it },
                        placeholder = { Text("Search test by name or code...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredTests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No unadded tests found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            items(filteredTests, key = { it.refId }) { testItem ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                            Text(testItem.testName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(color = Color(0xFFE6FFFA), shape = RoundedCornerShape(4.dp)) {
                                                    Text(testItem.testCode, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                                if (!testItem.testType.isNullOrBlank()) {
                                                    Text(testItem.testType, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                onAddTest(testItem.refId)
                                                showAddTestDialog = false
                                                testFilterQuery = ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddTestDialog = false
                    testFilterQuery = ""
                }) {
                    Text("Close")
                }
            }
        )
    }

    if (validationErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { validationErrorDialog = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Differential Percentage Error", fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorDialog ?: "") },
            confirmButton = {
                Button(
                    onClick = { validationErrorDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Correct Values")
                }
            }
        )
    }

    if (serverErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { serverErrorDialog = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clinical Submission Error", fontWeight = FontWeight.Bold) },
            text = { Text(serverErrorDialog ?: "") },
            confirmButton = {
                Button(onClick = { serverErrorDialog = null }) {
                    Text("Review")
                }
            }
        )
    }

    if (testPendingRemove != null) {
        val (testRefId, testTitle) = testPendingRemove!!
        AlertDialog(
            onDismissRequest = { testPendingRemove = null },
            title = { Text("Remove Test Panel?") },
            text = { Text("Remove \"$testTitle\" and its parameters from this report?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveTest(testRefId)
                        testPendingRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { testPendingRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!entryState.errorMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entryState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (entryState.statusMessage != null) {
            item {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entryState.statusMessage,
                        color = Color(0xFF166534),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (report != null) {
            report.tests.forEach { test ->
                val isCbcTest = test.testCode.trim().equals("CBC", ignoreCase = true)
                val cbcValidation = if (isCbcTest) {
                    ClinicalCalculations.validateCbcDifferential(test, entryState.parameterValues)
                } else null

                item(key = "test_header_${test.refId}") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Biotech,
                                            contentDescription = null,
                                            tint = Color(0xFF166534),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = test.testName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    color = Color(0xFFE6FFFA),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = test.testCode,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D9488),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }

                                Text(
                                    text = "(${test.parameters.size})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (report.tests.size > 1) {
                                IconButton(
                                    onClick = { testPendingRemove = test.refId to test.testName },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove panel",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Real-time CBC Differential Feedback Banner
                if (cbcValidation != null && cbcValidation.hasDifferential) {
                    item(key = "cbc_diff_status_${test.refId}") {
                        Surface(
                            color = if (cbcValidation.isValid) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (cbcValidation.isValid) Color(0xFF86EFAC) else MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (cbcValidation.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (cbcValidation.isValid) Color(0xFF166534) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (cbcValidation.isValid) {
                                        "Differential Sum: ${ClinicalCalculations.formatNumber(cbcValidation.sum)}% • Monocytes (Calculated): ${ClinicalCalculations.formatNumber(cbcValidation.mono ?: 0.0)}%"
                                    } else {
                                        cbcValidation.errorMessage ?: "Differential percentages exceed 100%."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (cbcValidation.isValid) Color(0xFF166534) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                items(test.parameters, key = { "${test.refId}_${it.parameterRefId}" }) { param ->
                    val isCurrentActive = activeParameterId == param.parameterRefId
                    val isCalculated = ClinicalCalculations.isCalculatedParameter(param, test.testCode)
                    val calculatedVal = if (isCalculated) {
                        ClinicalCalculations.calculateParameterValue(param, allParameters, entryState.parameterValues)
                    } else null
                    val resolvedValue = entryState.parameterValues[param.parameterRefId]?.takeIf { it.isNotBlank() }
                        ?: calculatedVal?.takeIf { it.isNotBlank() }
                        ?: param.value?.takeIf { it.isNotBlank() }
                        ?: ""

                    WorkspaceParameterCard(
                        parameter = param,
                        currentValue = resolvedValue,
                        calculatedFallback = calculatedVal,
                        testCode = test.testCode,
                        isActive = isCurrentActive,
                        onFocusChange = { activeParameterId = param.parameterRefId },
                        onValueChange = { newVal ->
                            activeParameterId = param.parameterRefId
                            onParameterChange(param.parameterRefId, newVal)
                        }
                    )
                }
            }

            // In-Step Test Addition Action (Lets user add another panel without losing current inputs!)
            item(key = "add_test_panel_button") {
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedButton(
                    onClick = { showAddTestDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)),
                    border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Another Test Panel", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }

            // Results Action Buttons Bar (placed naturally at the end of parameter list - no screen occlusion!)
            item(key = "results_action_buttons") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !entryState.isSaving && !entryState.isRecalculating,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onRecalculate,
                        enabled = !entryState.isRecalculating && !entryState.isSaving,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                    ) {
                        if (entryState.isRecalculating) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Recalculate",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recalculate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            // Pre-validate differential leukocyte count to prevent 400 Bad Request
                            val cbcErrors = report.tests.mapNotNull { test ->
                                val v = ClinicalCalculations.validateCbcDifferential(test, entryState.parameterValues)
                                if (v != null && !v.isValid) v.errorMessage else null
                            }

                            if (cbcErrors.isNotEmpty()) {
                                validationErrorDialog = cbcErrors.first()
                            } else {
                                onNext()
                            }
                        },
                        enabled = !entryState.isSaving && !entryState.isRecalculating,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        if (entryState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Preview →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun WorkspaceParameterCard(
    parameter: ReportParameterResultResponse,
    currentValue: String,
    calculatedFallback: String? = null,
    testCode: String? = null,
    isActive: Boolean,
    onFocusChange: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val isCalculated = ClinicalCalculations.isCalculatedParameter(parameter, testCode)
    val displayValue = currentValue.takeIf { it.isNotBlank() }
        ?: calculatedFallback?.takeIf { it.isNotBlank() }
        ?: parameter.value?.takeIf { it.isNotBlank() }
        ?: ""
    val refRangeText = if (parameter.referenceMin != null && parameter.referenceMax != null) {
        "${parameter.referenceMin} - ${parameter.referenceMax}"
    } else "—"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onFocusChange() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) BorderStroke(1.5.dp, Color(0xFF0D9488))
                 else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Parameter Name, Code Badge & Result Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = parameter.parameterName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 32.dp)
                    ) {
                        Text(
                            text = parameter.parameterCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
                ResultFlagBadge(flag = parameter.flag)
            }

            // Body Row: Input Field on Left, Reference Info on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isCalculated) {
                    val calcHint = ClinicalCalculations.getRequiredInputsHint(parameter)
                    val numVal = displayValue.toDoubleOrNull()
                    val isNegative = numVal != null && numVal < 0.0

                    Surface(
                        modifier = Modifier
                            .weight(1.1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNegative) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isNegative) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isNegative) Icons.Default.Warning else Icons.Default.Lock,
                                contentDescription = "Calculated",
                                tint = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(13.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isNegative -> "$displayValue (Exceeds 100%)"
                                        displayValue.isNotBlank() -> displayValue
                                        else -> "Auto"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isNegative -> MaterialTheme.colorScheme.error
                                        displayValue.isNotBlank() -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (displayValue.isBlank()) {
                                    Text(
                                        text = calcHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                color = if (isNegative) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = "CALC",
                                    color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = onValueChange,
                        placeholder = { Text("0.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Color(0xFF0D9488),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .defaultMinSize(minHeight = 48.dp)
                    )
                }

                // Reference Range & Unit Column
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (!parameter.unit.isNullOrBlank()) {
                        Text(
                            text = parameter.unit,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Ref: $refRangeText",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =========================================================================
// STEP 5: PRE-FINALIZATION REVIEW & LETTERHEAD TOGGLE (Step5ReviewReport.tsx)
// =========================================================================
@Composable
fun Step5PreFinalizationReview(
    report: ReportResponse?,
    includeLetterheadHeader: Boolean,
    errorMessage: String?,
    onToggleHeader: (Boolean) -> Unit,
    isFinalizing: Boolean,
    onBack: () -> Unit,
    onFinalizeClick: () -> Unit
) {
    val orgName = report?.organizationName?.ifBlank { "ARNAV PATHOLOGY LAB" } ?: "ARNAV PATHOLOGY LAB"
    val patientName = report?.displayPatientName ?: "MR Ram"
    val patientCode = report?.patientCode ?: "PAT-000001"
    val ageGender = report?.displayAgeGender ?: "35 years / MALE"
    val reportRef = report?.refId?.let { "RPT-${it.take(8).uppercase()}" } ?: "RPT-0gB0otGfnqZA"
    val reportDate = report?.createdAt?.take(16)?.replace("T", ", ") ?: "15 Nov 2026, 8:06 am"

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!errorMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Top Control Header Card (Compact & Balanced)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Pre-Finalization Review",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Review clinical A4 report before final sign-off.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Letterhead Header Pill Switch
                Surface(
                    color = if (includeLetterheadHeader) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, if (includeLetterheadHeader) Color(0xFF166534) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onToggleHeader(!includeLetterheadHeader) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (includeLetterheadHeader) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (includeLetterheadHeader) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (includeLetterheadHeader) "Letterhead ON" else "Letterhead OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (includeLetterheadHeader) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Simulated A4 Paper Sheet (White Paper Container with 1dp border)
        Surface(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A4 Header (or Reserved Blank Box when OFF)
                item {
                    if (includeLetterheadHeader) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE6FFFA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = orgName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0D9488)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = orgName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "CLINICAL & PATHOLOGICAL LABORATORY SERVICES",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D9488)
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "[ Pre-printed Letterhead Space Reserved (Header OFF) ]",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                }

                // Patient Metadata Bar (2x2 Grid Layout for Mobile)
                item {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Patient Name:", fontSize = 8.sp, color = Color.Gray)
                                    Text(patientName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Report Reference:", fontSize = 8.sp, color = Color.Gray)
                                    Text(reportRef, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Patient ID / MRN:", fontSize = 8.sp, color = Color.Gray)
                                    Text(patientCode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ordered / Created:", fontSize = 8.sp, color = Color.Gray)
                                    Text(reportDate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Age / Gender:", fontSize = 8.sp, color = Color.Gray)
                                    Text(ageGender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Status:", fontSize = 8.sp, color = Color.Gray)
                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) {
                                        Text("Draft", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Diagnostic Test Panels & Parameter Tables
                report?.tests?.forEach { test ->
                    item {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Biotech, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = test.testName.uppercase(),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E)
                                    )
                                }
                                Surface(color = Color(0xFFE6FFFA), shape = RoundedCornerShape(4.dp)) {
                                    Text(test.testCode, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp))
                                }
                            }
                        }
                    }

                    // Parameter Table Header (With Dedicated FLAG Column)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TEST PARAMETER", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(2.0f))
                            Text("OBSERVED", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.85f), textAlign = TextAlign.Center)
                            Text("UNIT", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.65f))
                            Text("REFERENCE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.0f))
                            Text("FLAG", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.25f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }

                    items(test.parameters) { param ->
                        val refText = if (param.referenceMin != null && param.referenceMax != null) {
                            "${param.referenceMin} - ${param.referenceMax}"
                        } else "—"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2.0f).padding(end = 4.dp)) {
                                Text(
                                    text = param.parameterName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (param.inputType.equals("CALCULATED", ignoreCase = true)) {
                                    Text("calc", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                }
                            }
                            Text(
                                text = param.value?.ifBlank { "—" } ?: "—",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.weight(0.85f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = param.unit ?: "—",
                                fontSize = 8.5.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(0.65f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = refText,
                                fontSize = 8.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1.0f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Box(
                                modifier = Modifier.weight(1.25f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                ResultFlagBadge(flag = param.flag)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }

                // Footer & Authorized Signatory Block
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Medical Examination Notes:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = report?.organizationReportDisclaimer?.ifBlank {
                                    "Test results relate only to the items tested. Please correlate clinically with patient symptoms."
                                } ?: "Test results relate only to the items tested. Please correlate clinically with patient symptoms.",
                                fontSize = 7.sp,
                                color = Color.Gray,
                                lineHeight = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF166534))
                            ) {
                                Text(
                                    text = "✓ Ready for Sign-off",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report?.organizationSignatureOwnerName?.ifBlank { "Authorized Signatory" } ?: "Authorized Signatory",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Consultant Pathologist, MD",
                                fontSize = 7.5.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar (Unclipped, Single-Line, Balanced)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isFinalizing,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Results", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }

            Button(
                onClick = onFinalizeClick,
                enabled = !isFinalizing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1.8f)
                    .height(44.dp)
            ) {
                if (isFinalizing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finalizing...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finalize & Sign", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
