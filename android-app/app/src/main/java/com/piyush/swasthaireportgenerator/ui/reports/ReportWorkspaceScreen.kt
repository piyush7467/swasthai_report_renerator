package com.piyush.swasthaireportgenerator.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
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
                        currentStep--
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 5-STEP STEPPER BAR (Matches Web Step Indicator!)
            WorkspaceStepperHeader(
                currentStep = currentStep,
                onStepClick = { step ->
                    if (step < currentStep || (step == 4 && activeReport != null)) {
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
                    onOpenAddPatient = { onNavigateBack() },
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
                    onRecalculate = { activeReport?.let { reportViewModel.saveAndRecalculate(it.refId) } },
                    onBack = { currentStep = 3 },
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
// 5-STEP WORKSPACE STEPPER HEADER (Matches Web ReportWorkspacePage.tsx)
// =========================================================================
@Composable
fun WorkspaceStepperHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf("Patient", "Doctor", "Tests", "Results", "Review")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isActive = currentStep == stepNumber
            val isCompleted = currentStep > stepNumber

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStepClick(stepNumber) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCompleted -> Color(0xFF10B981)
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNumber",
                                fontSize = 10.sp,
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
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> Color(0xFF047857)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
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

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedPatient != null) {
            Surface(
                color = Color(0xFFD1FAE5),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedPatient.salutation ?: ""} ${selectedPatient.name}".trim(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "UHID: ${selectedPatient.patientCode} • ${selectedPatient.ageValue ?: 0} ${selectedPatient.ageUnit ?: "Yrs"} • ${selectedPatient.gender ?: ""}",
                            color = Color(0xFF047857),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (!readOnly) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient by name, UHID, or phone...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onRefreshPatients) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Patients")
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
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "No patients match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        OutlinedButton(onClick = onOpenAddPatient, shape = RoundedCornerShape(10.dp)) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register New Patient")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { patient ->
                        val isSelected = selectedPatient?.refId == patient.refId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPatient(patient) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF0D9488)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "${patient.salutation ?: ""} ${patient.name}".trim(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "UHID: ${patient.patientCode} • ${patient.ageValue ?: 0} ${patient.ageUnit ?: "Yrs"} • ${patient.gender ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectPatient(patient) }
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
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (organizationProfile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = organizationProfile.organizationName ?: "Laboratory Facility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (organizationProfile.displayAddress.isNotBlank()) {
                        Text(
                            text = organizationProfile.displayAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!organizationProfile.signatureOwnerName.isNullOrBlank()) {
                        Text(
                            text = "Authorized Signatory: ${organizationProfile.signatureOwnerName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Text(
            text = "Assign Reporting Staff / Signatory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (signatoryOptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDoctorChange(doc) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF0D9488)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = doc,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(selected = isSelected, onClick = { onDoctorChange(doc) })
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = clinicalNotes,
            onValueChange = onNotesChange,
            label = { Text("Clinical Remarks / Sample Notes (Optional)") },
            placeholder = { Text("e.g. Fasting specimen, EDTA vial") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Text("← Back")
            }
            Button(onClick = onNext, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1.5f)) {
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
        Text(
            text = "Select Diagnostic Panels from Lab Catalog",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

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

        OutlinedTextField(
            value = searchTerm,
            onValueChange = { searchTerm = it },
            placeholder = { Text("Filter assigned tests (e.g. CBC, LFT, KFT)...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
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
                            text = "No active lab tests assigned to your organization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredTests) { test ->
                            val isAlreadyAdded = alreadyAddedTestRefIds.contains(test.testRefId)
                            val isChecked = isAlreadyAdded || selectedTestRefIds.contains(test.testRefId)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isAlreadyAdded) { onToggleTest(test.testRefId) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = if (isChecked) BorderStroke(1.5.dp, Color(0xFF0D9488)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${test.testName} (${test.testCode})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            if (isAlreadyAdded) "Already added to this report" else (test.testType ?: "Diagnostic Panel"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
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
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Text("← Back")
            }
            Button(
                onClick = onProceedToResults,
                enabled = totalEffectiveSelected > 0 && !isCreating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.8f)
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
    onRecalculate: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val report = entryState.report
    var testPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeParameter by remember { mutableStateOf<ReportParameterResultResponse?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!entryState.errorMessage.isNullOrBlank()) {
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

        if (entryState.statusMessage != null) {
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (report != null) {
                report.tests.forEach { test ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Biotech,
                                            contentDescription = null,
                                            tint = Color(0xFF166534),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = test.testName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            color = Color(0xFFE6FFFA),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = test.testCode,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0D9488),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${test.parameters.size} parameters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (report.tests.size > 1) {
                                    IconButton(
                                        onClick = { testPendingRemove = test.refId to test.testName },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove panel",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(test.parameters) { param ->
                        val isCurrentActive = activeParameter?.parameterRefId == param.parameterRefId
                        WorkspaceParameterCard(
                            parameter = param,
                            currentValue = entryState.parameterValues[param.parameterRefId] ?: "",
                            isActive = isCurrentActive,
                            onFocusChange = { activeParameter = param },
                            onValueChange = { newVal ->
                                activeParameter = param
                                onParameterChange(param.parameterRefId, newVal)
                            }
                        )
                    }
                }
            }
        }

        // Active Parameter Bottom Info Bar (Matching Web Screenshot!)
        val currentParam = activeParameter
        if (currentParam != null) {
            Surface(
                color = Color(0xFFE6FFFA),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF38B2AC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE PARAMETER: ${currentParam.parameterName} (${currentParam.parameterCode})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                        ResultFlagBadge(flag = currentParam.flag)
                    }
                    val rangeText = if (currentParam.referenceMin != null && currentParam.referenceMax != null) {
                        "${currentParam.referenceMin} - ${currentParam.referenceMax}"
                    } else "N/A"
                    Text(
                        text = "Unit: ${currentParam.unit ?: "—"}  •  Reference: $rangeText",
                        fontSize = 10.sp,
                        color = Color(0xFF115E59)
                    )
                }
            }
        }

        // Action Buttons Bar (Single-line, perfectly aligned)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !entryState.isSaving && !entryState.isRecalculating,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            OutlinedButton(
                onClick = onRecalculate,
                enabled = !entryState.isRecalculating && !entryState.isSaving,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
            ) {
                if (entryState.isRecalculating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Recalculate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recalculate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            Button(
                onClick = onNext,
                enabled = !entryState.isSaving && !entryState.isRecalculating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
            ) {
                if (entryState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Preview →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WorkspaceParameterCard(
    parameter: ReportParameterResultResponse,
    currentValue: String,
    isActive: Boolean,
    onFocusChange: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val isCalculated = parameter.inputType.equals("CALCULATED", ignoreCase = true)
    val refRangeText = if (parameter.referenceMin != null && parameter.referenceMax != null) {
        "${parameter.referenceMin} - ${parameter.referenceMax}"
    } else "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onFocusChange() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) BorderStroke(1.5.dp, Color(0xFF0D9488)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = parameter.parameterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = parameter.parameterCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                ResultFlagBadge(flag = parameter.flag)
            }

            // Body Row: Input Field on Left, Reference Info on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCalculated) {
                    Surface(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Calculated",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentValue.ifBlank { "Auto" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CALC",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = onValueChange,
                        placeholder = { Text("Enter value...", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Color(0xFF0D9488),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                // Reference Range & Unit Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!parameter.unit.isNullOrBlank()) {
                        Text(
                            text = parameter.unit,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Ref: $refRangeText",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Top Control Header Card (Matching Web Action Toolbar!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pre-Finalization Review",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Review A4 medical report preview before final sign-off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Letterhead Header Pill Switch
                Surface(
                    color = if (includeLetterheadHeader) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, if (includeLetterheadHeader) Color(0xFF166534) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onToggleHeader(!includeLetterheadHeader) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (includeLetterheadHeader) "Letterhead: ON ✓" else "Letterhead: OFF",
                            fontSize = 11.sp,
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
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 2.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .height(56.dp),
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
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Patient Name:", fontSize = 8.5.sp, color = Color.Gray)
                                    Text(patientName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Report Reference:", fontSize = 8.5.sp, color = Color.Gray)
                                    Text(reportRef, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Patient ID / MRN:", fontSize = 8.5.sp, color = Color.Gray)
                                    Text(patientCode, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ordered / Created:", fontSize = 8.5.sp, color = Color.Gray)
                                    Text(reportDate, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Age / Gender:", fontSize = 8.5.sp, color = Color.Gray)
                                    Text(ageGender, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Status:", fontSize = 8.5.sp, color = Color.Gray)
                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) {
                                        Text("Draft", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Biotech, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(15.dp))
                                    Text(
                                        text = test.testName.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E)
                                    )
                                }
                                Surface(color = Color(0xFFE6FFFA), shape = RoundedCornerShape(4.dp)) {
                                    Text(test.testCode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Parameter Table Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TEST PARAMETER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(2f))
                            Text("OBSERVED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("UNIT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.8f))
                            Text("REFERENCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.2f))
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(param.parameterName, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black, maxLines = 1)
                                if (param.inputType.equals("CALCULATED", ignoreCase = true)) {
                                    Text("calc", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                }
                            }
                            Text(param.value?.ifBlank { "—" } ?: "—", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(param.unit ?: "—", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.weight(0.8f))
                            Row(
                                modifier = Modifier.weight(1.2f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(refText, fontSize = 9.sp, color = Color.DarkGray)
                                ResultFlagBadge(flag = param.flag)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }

                // Footer & Authorized Signatory Block
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Medical Examination Notes:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(
                                text = report?.organizationReportDisclaimer?.ifBlank {
                                    "Test results relate only to the items tested. Please correlate clinically with patient symptoms."
                                } ?: "Test results relate only to the items tested. Please correlate clinically with patient symptoms.",
                                fontSize = 7.5.sp,
                                color = Color.Gray,
                                lineHeight = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF166534))
                            ) {
                                Text(
                                    text = "✓ Ready for Electronic Sign-off",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = report?.organizationSignatureOwnerName?.ifBlank { "Authorized Signatory" } ?: "Authorized Signatory",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Consultant Pathologist, MD",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isFinalizing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("← Results", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onFinalizeClick,
                enabled = !isFinalizing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.8f)
                    .height(48.dp)
            ) {
                if (isFinalizing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finalize & Sign Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
