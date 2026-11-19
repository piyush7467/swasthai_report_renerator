package com.piyush.swasthaireportgenerator.ui.reports

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.R
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.ReportShareResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizedReportViewScreen(
    reportRefId: String,
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    onOpenThemeDialog: () -> Unit = {}
) {
    val entryState by viewModel.entryState.collectAsState()
    val context = LocalContext.current

    // Header ON / OFF switch for pre-printed stationery (matching FinalizedReportView.tsx)
    var includeHeaderPreview by remember { mutableStateOf(true) }
    var showShareLinkDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(reportRefId) {
        viewModel.clearShareState()
        viewModel.clearPdfError()
        viewModel.loadReport(reportRefId)
        viewModel.loadFinalizedAssets(reportRefId)
    }

    LaunchedEffect(entryState.report?.refId) {
        entryState.report?.includeOrganizationHeader?.let {
            includeHeaderPreview = it
        }
    }

    if (showShareLinkDialog && entryState.report != null) {
        ShareReportLinkDialog(
            report = entryState.report!!,
            isGenerating = entryState.isSharingLink,
            shareResponse = entryState.shareResponse,
            errorMessage = entryState.pdfError,
            onGenerate = { channel, days ->
                viewModel.generateShareLink(reportRefId, channel, days)
            },
            onDismiss = {
                showShareLinkDialog = false
                viewModel.clearShareState()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "Certified Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Digitally Signed",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Reports")
                    }
                },
                actions = {
                    // Header Switch Pill in Top Bar (Matches Web Header: ON/OFF toggle!)
                    Surface(
                        onClick = { includeHeaderPreview = !includeHeaderPreview },
                        color = if (includeHeaderPreview) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (includeHeaderPreview) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (includeHeaderPreview) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (includeHeaderPreview) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (includeHeaderPreview) "Header ON" else "Header OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (includeHeaderPreview) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Online Link") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showShareLinkDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Report") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.loadReport(reportRefId)
                                    viewModel.loadFinalizedAssets(reportRefId)
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
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.downloadPdfAndGetUri(context, report.refId, includeHeaderPreview) { uri ->
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(viewIntent)
                                    } catch (_: Throwable) {
                                        Toast.makeText(
                                            context,
                                            "No PDF viewer installed on device. Use Share PDF instead.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !entryState.isPdfDownloading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Open PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.downloadPdfAndGetUri(context, report.refId, includeHeaderPreview) { uri ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "SwasthAI Official Diagnostic Report - ${report.displayPatientName}")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Official Certified Diagnostic Report for ${report.displayPatientName} (Ref: ${report.refId.take(10)})"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Official PDF Report"))
                                    } catch (_: Throwable) {
                                        Toast.makeText(context, "No app available to share PDF report.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !entryState.isPdfDownloading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (entryState.isPdfDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { showShareLinkDialog = true },
                            enabled = !entryState.isPdfDownloading,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "Create Share Link")
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
                item { Spacer(modifier = Modifier.height(2.dp)) }

                if (!entryState.pdfError.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entryState.pdfError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearPdfError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Authenticated Status Banner
                item {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Certified",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Finalized & Authenticated",
                                    color = Color(0xFF065F46),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Certified by ${report.finalizedByName ?: "Pathologist"} • Tamper-proof signed",
                                    color = Color(0xFF047857),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // 2. Organization Header Block (or Pre-printed Stationery Reserved Blank Space when Header is OFF)
                item {
                    if (includeHeaderPreview) {
                        val orgProfile = entryState.organizationProfile
                        val labName = report.organizationName?.takeIf { it.isNotBlank() }
                            ?: orgProfile?.organizationName?.takeIf { it.isNotBlank() }
                            ?: "Arnav Pathology Lab"

                        val labAddress = run {
                            val line1 = report.organizationAddressLine1?.takeIf { it.isNotBlank() }
                                ?: orgProfile?.addressLine1?.takeIf { it.isNotBlank() }
                            val line2 = report.organizationAddressLine2?.takeIf { it.isNotBlank() }
                                ?: orgProfile?.addressLine2?.takeIf { it.isNotBlank() }
                            val city = report.organizationCity?.takeIf { it.isNotBlank() }
                                ?: orgProfile?.city?.takeIf { it.isNotBlank() }
                            val state = report.organizationState?.takeIf { it.isNotBlank() }
                                ?: orgProfile?.state?.takeIf { it.isNotBlank() }
                            val pincode = report.organizationPostalCode?.takeIf { it.isNotBlank() }
                                ?: orgProfile?.postalCode?.takeIf { it.isNotBlank() }
                            val statePin = listOfNotNull(state, pincode).filter { it.isNotBlank() }.joinToString(" - ")
                            listOfNotNull(line1, line2, city, statePin.takeIf { it.isNotBlank() })
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                        }

                        val labPhone = report.organizationPhone?.takeIf { it.isNotBlank() }
                            ?: orgProfile?.phone?.takeIf { it.isNotBlank() }
                            ?: orgProfile?.alternatePhone?.takeIf { it.isNotBlank() }

                        val labEmail = report.organizationEmail?.takeIf { it.isNotBlank() }
                            ?: orgProfile?.email?.takeIf { it.isNotBlank() }

                        val labWebsite = report.organizationWebsite?.takeIf { it.isNotBlank() }
                            ?: orgProfile?.website?.takeIf { it.isNotBlank() }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        val logoBitmap = remember(entryState.logoBytes) {
                                            entryState.logoBytes?.let { bytes ->
                                                try {
                                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                                } catch (_: Throwable) {
                                                    null
                                                }
                                            }
                                        }
                                        if (logoBitmap != null) {
                                            Image(
                                                bitmap = logoBitmap,
                                                contentDescription = "Laboratory Logo",
                                                modifier = Modifier.padding(4.dp).fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_swasthai_logo),
                                                contentDescription = "Logo",
                                                modifier = Modifier.padding(6.dp).fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = labName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Accredited Diagnostic & Clinical Laboratory",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (labAddress.isNotBlank() || !labPhone.isNullOrBlank() || !labEmail.isNullOrBlank() || !labWebsite.isNullOrBlank()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    if (labAddress.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = "Address",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp).padding(top = 1.dp)
                                            )
                                            Text(
                                                text = labAddress,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    val contacts = listOfNotNull(
                                        labPhone?.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                                        labEmail?.takeIf { it.isNotBlank() }?.let { "✉️ $it" },
                                        labWebsite?.takeIf { it.isNotBlank() }?.let { "🌐 $it" }
                                    )
                                    if (contacts.isNotEmpty()) {
                                        Text(
                                            text = contacts.joinToString("  •  "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Header OFF: Blank space placeholder for physical letterhead stationery
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "[ Pre-printed Letterhead Space Reserved (Header OFF) ]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Patient Clinical Demographics Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section Header: Title + UHID Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "PATIENT CLINICAL DEMOGRAPHICS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                }

                                val uhid = report.patientCode?.takeIf { it.isNotBlank() } ?: "UHID: —"
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (uhid.startsWith("UHID", ignoreCase = true)) uhid else "UHID: $uhid",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                            // Grid Row 1: Patient Name & Report Ref ID
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DemographicField(
                                    label = "PATIENT NAME",
                                    value = report.displayPatientName,
                                    isBold = true,
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.1f)
                                )
                                DemographicField(
                                    label = "REPORT REF ID",
                                    value = report.refId.take(14),
                                    isBold = true,
                                    valueColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            // Grid Row 2: Age / Gender & Registered Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DemographicField(
                                    label = "AGE • GENDER",
                                    value = report.displayAgeGender,
                                    modifier = Modifier.weight(1.1f)
                                )
                                val displayDate = report.createdAt?.take(10) ?: report.finalizedAt?.take(10) ?: "—"
                                DemographicField(
                                    label = "DATE OF REPORT",
                                    value = displayDate,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            // Grid Row 3: Patient Contact & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DemographicField(
                                    label = "CONTACT NUMBER",
                                    value = report.patientPhone?.takeIf { it.isNotBlank() } ?: "—",
                                    modifier = Modifier.weight(1.1f)
                                )
                                DemographicField(
                                    label = "CERTIFICATION",
                                    value = "FINALIZED & AUTHENTICATED",
                                    valueColor = Color(0xFF10B981),
                                    isBold = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                }

                // 4. Test Panels & Results
                report.tests.forEach { test ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
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
                                    text = test.testName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (test.parameters.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${test.parameters.size} ${if (test.parameters.size == 1) "parameter" else "parameters"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(test.parameters) { param ->
                        FinalizedParameterRow(param = param)
                    }
                }

                // 5. Verification Seal, Digital Signature & Real QR Code Footer
                item {
                    val qrBitmap = remember(entryState.qrBytes) {
                        entryState.qrBytes?.let { bytes ->
                            try {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    }
                    val sigBitmap = remember(entryState.signatureBytes) {
                        entryState.signatureBytes?.let { bytes ->
                            try {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    }
                    val footerNote = report.organizationReportFooterText?.takeIf { it.isNotBlank() }
                        ?: entryState.organizationProfile?.reportFooterText?.takeIf { it.isNotBlank() }
                        ?: entryState.organizationProfile?.reportDisclaimer?.takeIf { it.isNotBlank() }
                        ?: "This is an electronically validated and certified clinical report. Authenticity can be verified by scanning the QR code above."

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (sigBitmap != null) {
                                        Image(
                                            bitmap = sigBitmap,
                                            contentDescription = "Authorized Signature",
                                            modifier = Modifier
                                                .height(44.dp)
                                                .widthIn(max = 140.dp)
                                                .background(Color.White, RoundedCornerShape(6.dp))
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Text(
                                        text = "Electronically Verified & Signed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Signed By: ${report.organizationSignatureOwnerName ?: report.finalizedByName ?: "Authorized Signatory"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap,
                                            contentDescription = "Report Verification QR Code",
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "Verification QR",
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (!footerNote.isNullOrBlank()) {
                                HorizontalDivider()
                                Text(
                                    text = footerNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun ShareReportLinkDialog(
    report: ReportResponse,
    isGenerating: Boolean,
    shareResponse: ReportShareResponse?,
    errorMessage: String?,
    onGenerate: (channel: String, expiresInDays: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedChannel by remember { mutableStateOf("LINK") }
    var selectedDays by remember { mutableIntStateOf(7) }

    val channels = listOf("LINK", "WHATSAPP", "EMAIL")
    val expiryOptions = listOf(7, 30, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Create Secure Share Link",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Generate an expiring verification link for ${report.displayPatientName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Channel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    channels.forEach { channel ->
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = { selectedChannel = channel },
                            label = { Text(channel, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Expires After", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    expiryOptions.forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { selectedDays = days },
                            label = { Text("$days days", fontSize = 11.sp) }
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (shareResponse != null) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = shareResponse.shareUrl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            if (!shareResponse.expiresAt.isNullOrBlank()) {
                                Text(
                                    text = "Expires: ${shareResponse.expiresAt.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("SwasthAI Report Link", shareResponse.shareUrl))
                                        Toast.makeText(context, "Share link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Copy Link", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareResponse.shareUrl)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(sendIntent, "Send Report Link"))
                                        } catch (_: Throwable) {
                                            Toast.makeText(context, "No app available to share link.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Share URL", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedChannel, selectedDays) },
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (shareResponse == null) "Generate Link" else "Regenerate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DemographicField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = if (isBold) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

