package com.piyush.swasthaireportgenerator.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.PatientResponse

data class AvailableTest(
    val code: String,
    val name: String,
    val description: String
)

val DEFAULT_TEST_PANELS = listOf(
    AvailableTest("CBC", "Complete Blood Count", "Hemoglobin, RBC, WBC, Platelets, Differential"),
    AvailableTest("LFT", "Liver Function Test", "Bilirubin, SGOT/AST, SGPT/ALT, Proteins, A/G Ratio"),
    AvailableTest("KFT", "Kidney Function Test", "Urea, Creatinine, BUN, Uric Acid, eGFR")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportDialog(
    patients: List<PatientResponse>,
    preselectedPatient: PatientResponse? = null,
    onDismissRequest: () -> Unit,
    onCreateReport: (patientRefId: String, testCodes: List<String>) -> Unit,
    isCreating: Boolean
) {
    var selectedPatient by remember { mutableStateOf(preselectedPatient ?: patients.firstOrNull()) }
    val selectedTests = remember { mutableStateListOf("LFT") }
    var expandedPatientDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Create Diagnostic Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 1. Select Patient
                Text(
                    text = "Select Patient *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ExposedDropdownMenuBox(
                    expanded = expandedPatientDropdown,
                    onExpandedChange = { expandedPatientDropdown = !expandedPatientDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.let { "${it.name} (${it.patientCode})" } ?: "Choose Patient",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPatientDropdown) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedPatientDropdown,
                        onDismissRequest = { expandedPatientDropdown = false }
                    ) {
                        if (patients.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No patients registered yet") },
                                onClick = { expandedPatientDropdown = false }
                            )
                        } else {
                            patients.forEach { patient ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(patient.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "UHID: ${patient.patientCode} • ${patient.gender ?: ""} ${patient.ageValue ?: 0}Yrs",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPatient = patient
                                        expandedPatientDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 2. Select Test Panels
                Text(
                    text = "Select Test Panels *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                DEFAULT_TEST_PANELS.forEach { test ->
                    val isChecked = selectedTests.contains(test.code)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    if (selectedTests.size > 1) {
                                        selectedTests.remove(test.code)
                                    }
                                } else {
                                    selectedTests.add(test.code)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${test.name} (${test.code})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = test.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedTests.add(test.code)
                                    } else if (selectedTests.size > 1) {
                                        selectedTests.remove(test.code)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val patient = selectedPatient
                    if (patient == null) {
                        errorMessage = "Please select a patient"
                        return@Button
                    }
                    if (selectedTests.isEmpty()) {
                        errorMessage = "Please select at least one test panel"
                        return@Button
                    }
                    errorMessage = null
                    onCreateReport(patient.refId, selectedTests.toList())
                },
                enabled = !isCreating,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Draft Worklist")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
