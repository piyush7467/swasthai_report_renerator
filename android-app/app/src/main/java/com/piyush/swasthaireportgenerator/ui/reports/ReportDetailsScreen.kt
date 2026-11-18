package com.piyush.swasthaireportgenerator.ui.reports

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.ReportParameterResultResponse
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar

@Composable
fun ReportDetailsScreen(
    reportRefId: String,
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    onOpenThemeDialog: () -> Unit
) {
    val entryState by viewModel.entryState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(reportRefId) {
        viewModel.loadReport(reportRefId)
    }

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Official Report",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onOpenThemeDialog = onOpenThemeDialog
            )
        },
        bottomBar = {
            if (entryState.report != null) {
                val report = entryState.report!!
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
                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SwasthAI Lab Report - ${report.displayPatientName}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Official Diagnostic Report for ${report.displayPatientName} (Ref: ${report.refId})\n" +
                                        "Laboratory: ${report.organizationName}\n" +
                                        "Status: Finalized & Digitally Signed\n" +
                                        "Finalized At: ${report.finalizedAt ?: "Certified"}"
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Lab Report via"))
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "No app available to share report.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Report (WhatsApp / Drive)", fontWeight = FontWeight.Bold)
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

                // Authentication Status Banner
                item {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Authenticated",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Finalized & Authenticated",
                                    color = Color(0xFF065F46),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Electronically signed by ${report.finalizedByName ?: "Pathologist"}",
                                    color = Color(0xFF047857),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Patient Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Patient Information",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = report.displayPatientName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "UHID: ${report.patientCode ?: ""} • ${report.displayAgeGender}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Organization: ${report.organizationName ?: "Diagnostic Center"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Test Panels and Values
                report.tests.forEach { test ->
                    item {
                        Text(
                            text = test.testName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(test.parameters) { param ->
                        FinalizedParameterRow(param = param)
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun FinalizedParameterRow(param: ReportParameterResultResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = param.parameterName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val rangeText = if (param.referenceMin != null && param.referenceMax != null) {
                    "Ref: ${param.referenceMin} - ${param.referenceMax}"
                } else ""

                if (rangeText.isNotBlank()) {
                    Text(
                        text = rangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 80.dp)
                    .wrapContentWidth()
            ) {
                ResultFlagBadge(flag = param.flag)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = param.value?.takeIf { it.isNotBlank() } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!param.unit.isNullOrBlank()) {
                        Text(
                            text = param.unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
