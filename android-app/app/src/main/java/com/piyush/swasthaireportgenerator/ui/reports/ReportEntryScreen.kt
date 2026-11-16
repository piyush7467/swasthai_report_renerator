package com.piyush.swasthaireportgenerator.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.ReportParameterResultResponse
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar

@Composable
fun ReportEntryScreen(
    reportRefId: String,
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    onFinalizeSuccess: () -> Unit,
    onOpenThemeDialog: () -> Unit
) {
    val entryState by viewModel.entryState.collectAsState()
    var showFinalizeConfirmDialog by remember { mutableStateOf(false) }
    var testPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (testPendingRemove != null) {
        val (testRefId, testTitle) = testPendingRemove!!
        AlertDialog(
            onDismissRequest = { testPendingRemove = null },
            title = { Text("Remove Test Panel?") },
            text = { Text("Remove \"$testTitle\" and its entered values from this draft report?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeTest(reportRefId, testRefId)
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

    LaunchedEffect(reportRefId) {
        viewModel.loadReport(reportRefId)
    }

    if (showFinalizeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizeConfirmDialog = false },
            title = { Text("Finalize & Sign Report?") },
            text = {
                Text("Once finalized, the report will be electronically certified and parameter values will be locked.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalizeConfirmDialog = false
                        viewModel.finalizeReport(reportRefId, onFinalizeSuccess)
                    }
                ) {
                    Text("Confirm & Sign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Result Entry",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onOpenThemeDialog = onOpenThemeDialog
            )
        },
        bottomBar = {
            if (entryState.report != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Recalculate Button
                        OutlinedButton(
                            onClick = {
                                viewModel.saveAndRecalculate(reportRefId)
                            },
                            enabled = !entryState.isRecalculating && !entryState.isFinalizing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (entryState.isRecalculating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Calculate, contentDescription = "Recalculate")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recalculate", fontSize = 12.sp)
                            }
                        }

                        // Finalize & Sign Button
                        Button(
                            onClick = { showFinalizeConfirmDialog = true },
                            enabled = !entryState.isFinalizing && !entryState.isRecalculating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (entryState.isFinalizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Verified, contentDescription = "Sign")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finalize & Sign", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (entryState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (entryState.report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(entryState.errorMessage ?: "Report not found")
            }
        } else {
            val report = entryState.report!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Patient Info Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = report.displayPatientName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "UHID: ${report.patientCode ?: ""} • ${report.displayAgeGender}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Success / Status Message
                if (entryState.statusMessage != null) {
                    item {
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entryState.statusMessage!!,
                                color = Color(0xFF065F46),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Parameter Entry Sections by Test Panel
                report.tests.forEach { test ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${test.testName} (${test.testCode})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (report.tests.size > 1) {
                                IconButton(
                                    onClick = { testPendingRemove = test.refId to test.testName },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove test panel",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(test.parameters) { param ->
                        ParameterEntryRow(
                            parameter = param,
                            currentValue = entryState.parameterValues[param.parameterRefId] ?: "",
                            onValueChange = { newVal ->
                                viewModel.onParameterChange(param.parameterRefId, newVal)
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun ParameterEntryRow(
    parameter: ReportParameterResultResponse,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    val isCalculated = parameter.inputType.equals("CALCULATED", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCalculated) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Parameter Name & Reference Interval
            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = parameter.parameterName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCalculated) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
                    ResultFlagBadge(flag = parameter.flag)
                }

                // Reference Range & Unit
                val rangeText = if (parameter.referenceMin != null && parameter.referenceMax != null) {
                    "Ref: ${parameter.referenceMin} - ${parameter.referenceMax} ${parameter.unit ?: ""}"
                } else {
                    parameter.unit ?: ""
                }

                if (rangeText.isNotBlank()) {
                    Text(
                        text = rangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Input Box
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChange,
                enabled = !isCalculated,
                placeholder = { Text(if (isCalculated) "Auto" else "Value") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ResultFlagBadge(
    flag: String?,
    modifier: Modifier = Modifier
) {
    val clean = flag?.trim()?.uppercase().orEmpty()
    if (clean.isEmpty()) return

    val (label, bg, fg) = when (clean) {
        "HIGH", "H" -> Triple("HIGH", Color(0xFFFFEDD5), Color(0xFFC2410C))
        "LOW", "L" -> Triple("LOW", Color(0xFFFEF3C7), Color(0xFF92400E))
        "CRITICAL", "CRITICAL_HIGH", "CRITICAL_LOW" -> Triple("CRITICAL", Color(0xFFFEE2E2), Color(0xFFB91C1C))
        "NORMAL", "N" -> Triple("NORMAL", Color(0xFFDCFCE7), Color(0xFF166534))
        else -> Triple(clean, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        modifier = modifier.wrapContentSize(),
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
        )
    }
}
