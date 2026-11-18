package com.piyush.swasthaireportgenerator.ui.organization

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
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
import com.piyush.swasthaireportgenerator.ui.components.SignaturePadDialog
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationSettingsScreen(
    viewModel: OrganizationProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isOrgAdmin = uiState.userRole.equals("ORG_ADMIN", ignoreCase = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    var deleteConfirmTarget by remember { mutableStateOf<String?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadLogoFromUri(context, uri)
        }
    }

    val signaturePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadSignatureFromUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    if (deleteConfirmTarget != null) {
        val target = deleteConfirmTarget!!
        val label = if (target == "logo") "Laboratory Logo" else "Authorized Signature"
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text("Remove $label?") },
            text = {
                Text("This will remove the $label from future PDF reports and verification headers. You can re-upload one anytime.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target == "logo") viewModel.deleteLogo() else viewModel.deleteSignature()
                        deleteConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            isSaving = uiState.isUploadingSignature,
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { bytes ->
                viewModel.uploadSignatureBytes(bytes)
                showSignaturePad = false
            }
        )
    }

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Lab Profile & Branding",
                subtitle = if (isOrgAdmin) "Letterhead, contact & digital signatory" else "Organization identity (read-only)",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Settings Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Profile") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.loadProfile()
                                }
                            )
                            if (isOrgAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Draw Signature") },
                                    leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSignaturePad = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload Signature Image") },
                                    leadingIcon = { Icon(Icons.Default.DriveFolderUpload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        signaturePicker.launch("image/*")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload Lab Logo") },
                                    leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        logoPicker.launch("image/*")
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = uiState.profile

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isOrgAdmin) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Read-only",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Only a Laboratory Owner (ORG_ADMIN) can edit organization profile settings or change letterhead branding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
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
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(28.dp)
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

            if (!uiState.successMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFDCFCE7),
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
                            contentDescription = "Saved",
                            tint = Color(0xFF15803D)
                        )
                        Text(
                            text = uiState.successMessage!!,
                            color = Color(0xFF166534),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Lab Identity Header Card
            if (profile != null) {
                OrganizationIdentitySummaryCard(profile = profile, logoBytes = uiState.logoBytes)
            }

            // Four Tabs matching Web UI
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("General & Address") },
                    icon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Logo & Signature") },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Report Footers") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Letterhead Live Preview") },
                    icon = { Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (profile != null) {
                when (selectedTab) {
                    0 -> GeneralAndAddressSection(
                        profile = profile,
                        isOrgAdmin = isOrgAdmin,
                        isSaving = uiState.isSaving,
                        onSave = { a1, a2, city, st, zip, country, phone, altPhone, email, web ->
                            viewModel.saveProfile(
                                addressLine1 = a1,
                                addressLine2 = a2,
                                city = city,
                                state = st,
                                postalCode = zip,
                                country = country,
                                phone = phone,
                                alternatePhone = altPhone,
                                email = email,
                                website = web,
                                reportFooterText = profile.reportFooterText ?: "",
                                reportDisclaimer = profile.reportDisclaimer ?: ""
                            )
                        }
                    )
                    1 -> BrandingAndSignatureSection(
                        profile = profile,
                        logoBytes = uiState.logoBytes,
                        signatureBytes = uiState.signatureBytes,
                        isOrgAdmin = isOrgAdmin,
                        isUploadingLogo = uiState.isUploadingLogo,
                        isUploadingSignature = uiState.isUploadingSignature,
                        onPickLogo = { logoPicker.launch("image/*") },
                        onDeleteLogo = { deleteConfirmTarget = "logo" },
                        onDrawSignature = { showSignaturePad = true },
                        onPickSignature = { signaturePicker.launch("image/*") },
                        onDeleteSignature = { deleteConfirmTarget = "signature" }
                    )
                    2 -> ReportFootersSection(
                        profile = profile,
                        isOrgAdmin = isOrgAdmin,
                        isSaving = uiState.isSaving,
                        onSave = { footer, disclaimer ->
                            viewModel.saveProfile(
                                addressLine1 = profile.addressLine1 ?: "",
                                addressLine2 = profile.addressLine2 ?: "",
                                city = profile.city ?: "",
                                state = profile.state ?: "",
                                postalCode = profile.postalCode ?: "",
                                country = profile.country ?: "",
                                phone = profile.phone ?: "",
                                alternatePhone = profile.alternatePhone ?: "",
                                email = profile.email ?: "",
                                website = profile.website ?: "",
                                reportFooterText = footer,
                                reportDisclaimer = disclaimer
                            )
                        }
                    )
                    3 -> LetterheadLivePreviewSection(
                        profile = profile,
                        logoBytes = uiState.logoBytes,
                        signatureBytes = uiState.signatureBytes
                    )
                }
            }

            // Bottom Spacing for Keyboard Avoidance
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun OrganizationIdentitySummaryCard(
    profile: OrganizationProfileResponse,
    logoBytes: ByteArray?
) {
    val logoBitmap = remember(logoBytes) {
        logoBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = "Lab Logo",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "Lab",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.organizationName ?: "Diagnostic Laboratory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile.displayAddress.ifBlank { "Address not configured yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!profile.phone.isNullOrBlank() || !profile.email.isNullOrBlank()) {
                    Text(
                        text = listOfNotNull(profile.phone, profile.email)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralAndAddressSection(
    profile: OrganizationProfileResponse,
    isOrgAdmin: Boolean,
    isSaving: Boolean,
    onSave: (String, String, String, String, String, String, String, String, String, String) -> Unit
) {
    var addressLine1 by remember(profile) { mutableStateOf(profile.addressLine1 ?: "") }
    var addressLine2 by remember(profile) { mutableStateOf(profile.addressLine2 ?: "") }
    var city by remember(profile) { mutableStateOf(profile.city ?: "") }
    var state by remember(profile) { mutableStateOf(profile.state ?: "") }
    var postalCode by remember(profile) { mutableStateOf(profile.postalCode ?: "") }
    var country by remember(profile) { mutableStateOf(profile.country ?: "") }
    var phone by remember(profile) { mutableStateOf(profile.phone ?: "") }
    var alternatePhone by remember(profile) { mutableStateOf(profile.alternatePhone ?: "") }
    var email by remember(profile) { mutableStateOf(profile.email ?: "") }
    var website by remember(profile) { mutableStateOf(profile.website ?: "") }

    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Laboratory Identity & Contacts Card (Matching screenshot)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Laboratory Identity & Contacts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Official contact details that appear on printed reports and patient verification pages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = profile.organizationName ?: "",
                    onValueChange = {},
                    label = { Text("Organization Name") },
                    enabled = false,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tenant name configured in platform licensing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Official Lab Email") },
                    placeholder = { Text("reports@laboratory.com") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Vertically aligned contact fields
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Primary Phone") },
                    placeholder = { Text("+91 98765 43210") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = alternatePhone,
                    onValueChange = { alternatePhone = it },
                    label = { Text("Alternate / Emergency Contact") },
                    placeholder = { Text("+91 11 2345 6789") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website URL") },
                    placeholder = { Text("https://www.laboratory.com") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Laboratory Address Card (All fields aligned vertically)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Laboratory Address",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = it },
                    label = { Text("Address Line 1") },
                    placeholder = { Text("Street address or building") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = addressLine2,
                    onValueChange = { addressLine2 = it },
                    label = { Text("Address Line 2") },
                    placeholder = { Text("Suite, floor, or landmark") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    placeholder = { Text("City") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    placeholder = { Text("State") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    placeholder = { Text("PIN code") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    placeholder = { Text("Country") },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedPlaceholderColor = placeholderColor, focusedPlaceholderColor = placeholderColor),
                    enabled = isOrgAdmin,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOrgAdmin) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onSave(addressLine1, addressLine2, city, state, postalCode, country, phone, alternatePhone, email, website)
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save General & Address Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandingAndSignatureSection(
    profile: OrganizationProfileResponse,
    logoBytes: ByteArray?,
    signatureBytes: ByteArray?,
    isOrgAdmin: Boolean,
    isUploadingLogo: Boolean,
    isUploadingSignature: Boolean,
    onPickLogo: () -> Unit,
    onDeleteLogo: () -> Unit,
    onDrawSignature: () -> Unit,
    onPickSignature: () -> Unit,
    onDeleteSignature: () -> Unit
) {
    val logoBitmap = remember(logoBytes) {
        logoBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }
    val signatureBitmap = remember(signatureBytes) {
        signatureBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Laboratory Logo Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Laboratory Logo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(
                        label = if (profile.logoConfigured) "CONFIGURED" else "NOT SET",
                        containerColor = if (profile.logoConfigured) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (profile.logoConfigured) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Printed on report headers, sidebar branding, and shared PDFs (PNG/JPG, max 2 MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingLogo) {
                        CircularProgressIndicator()
                    } else if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = "Laboratory Logo Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "No Logo",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No logo uploaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isOrgAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPickLogo,
                            enabled = !isUploadingLogo,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (profile.logoConfigured) "Replace Logo" else "Upload Logo")
                        }
                        if (profile.logoConfigured) {
                            OutlinedButton(
                                onClick = onDeleteLogo,
                                enabled = !isUploadingLogo,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Logo", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Authorized Signature Card (Upload File OR Draw in App)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Authorized Signature",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val verStatus = (profile.signatureVerificationStatus ?: "PENDING").uppercase()
                    val (bg, fg) = when (verStatus) {
                        "APPROVED" -> Color(0xFFDCFCE7) to Color(0xFF166534)
                        "REJECTED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                    }
                    StatusChip(label = verStatus, containerColor = bg, contentColor = fg)
                }

                Text(
                    text = "Pathologist digital signature stamped on certified reports. You can draw directly in the app or upload an existing image.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!profile.signatureOwnerName.isNullOrBlank() || !profile.signatureOwnerEmail.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Signatory: ${profile.signatureOwnerName ?: "-"} ${profile.signatureOwnerEmail?.let { "($it)" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Verification Status Info Banners (matching Web UI)
                val verStatus = (profile.signatureVerificationStatus ?: "PENDING").uppercase()
                if (profile.signatureConfigured && verStatus == "PENDING") {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Signature Pending Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Your digital signature specimen has been submitted to platform administration for clinical verification. Reports cannot be certified until approved.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else if (profile.signatureConfigured && verStatus == "APPROVED") {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Verified & Active for Reports",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Verified by platform administration. This signature is automatically affixed to doctor-signed finalized PDF reports.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else if (profile.signatureConfigured && verStatus == "REJECTED") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Signature Verification Rejected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Reason: ${profile.signatureRejectionReason ?: "Signature did not meet clinical compliance requirements."} Please draw or upload a replacement.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Signature Specimen Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingSignature) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            Text("Saving signature...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    } else if (signatureBitmap != null) {
                        Image(
                            bitmap = signatureBitmap,
                            contentDescription = "Authorized Signature Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = "No Signature",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No signature uploaded",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Draw in app or upload transparent PNG",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Two Signature Methods: Draw in App | Upload File | Delete
                if (isOrgAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Draw in App button (Primary)
                        Button(
                            onClick = onDrawSignature,
                            enabled = !isUploadingSignature,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (profile.signatureConfigured) "Draw New" else "Draw Signature")
                        }

                        // 2. Upload Image button (Secondary)
                        OutlinedButton(
                            onClick = onPickSignature,
                            enabled = !isUploadingSignature,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload File")
                        }

                        // 3. Delete button (if configured)
                        if (profile.signatureConfigured) {
                            OutlinedButton(
                                onClick = onDeleteSignature,
                                enabled = !isUploadingSignature,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Signature", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFootersSection(
    profile: OrganizationProfileResponse,
    isOrgAdmin: Boolean,
    isSaving: Boolean,
    onSave: (String, String) -> Unit
) {
    var reportFooterText by remember(profile) { mutableStateOf(profile.reportFooterText ?: "") }
    var reportDisclaimer by remember(profile) { mutableStateOf(profile.reportDisclaimer ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Report Footer & Clinical Disclaimer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Custom footer notes and legal clinical disclaimers printed at the bottom of pathology test reports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = reportFooterText,
                onValueChange = { reportFooterText = it },
                label = { Text("Report Footer Note") },
                enabled = isOrgAdmin,
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = reportDisclaimer,
                onValueChange = { reportDisclaimer = it },
                label = { Text("Clinical Disclaimer") },
                enabled = isOrgAdmin,
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (isOrgAdmin) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        onSave(reportFooterText, reportDisclaimer)
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Report Footers", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterheadLivePreviewSection(
    profile: OrganizationProfileResponse,
    logoBytes: ByteArray?,
    signatureBytes: ByteArray?
) {
    val logoBitmap = remember(logoBytes) {
        logoBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }
    val signatureBitmap = remember(signatureBytes) {
        signatureBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    val orgName = profile.organizationName?.ifBlank { "Diagnostic Laboratory" } ?: "Diagnostic Laboratory"
    val firstLetter = orgName.take(1).uppercase()
    val addressText = profile.displayAddress.ifBlank { "Laboratory Address Line 1, City, State - PIN" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header description & A4 badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Letterhead Live Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time mobile simulation of report header, contact, footer & signature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color(0xFFE6FFFA),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0xFF38B2AC))
                ) {
                    Text(
                        text = "A4 Format",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C7A7B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Simulated A4 Paper Sheet (Reduced padding around org name)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Header Row (Logo + Org Name + Address with reduced padding)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6FFFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(3.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C7A7B)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = orgName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1
                            )
                            Text(
                                text = "CLINICAL & PATHOLOGICAL LABORATORY SERVICES",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488),
                                maxLines = 1
                            )
                            Text(
                                text = addressText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 2
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // 2. Patient Details Bar (2x2 Grid for mobile responsiveness)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PATIENT NAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("John Doe (Sample)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AGE / GENDER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("34 Yrs / Male", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PID CODE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("PID-847291", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DATE OF REPORT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("14 Nov 2026", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                                }
                            }
                        }
                    }

                    // 3. Investigation Table
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("INVESTIGATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(2f))
                            Text("OBSERVED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.3f))
                            Text("REFERENCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.3f))
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hemoglobin (Hb)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(2f))
                            Text("14.8 g/dL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1.3f))
                            Text("13.0-17.0", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.weight(1.3f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Leukocyte (TLC)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(2f))
                            Text("7,200 /uL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1.3f))
                            Text("4k-11k", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.weight(1.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                    // 4. Footer & Signature Area
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "This report is digitally generated and electronically signed under the Information Technology Act.",
                            fontSize = 7.5.sp,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (signatureBitmap != null) {
                                    Image(
                                        bitmap = signatureBitmap,
                                        contentDescription = "Doctor Signature",
                                        modifier = Modifier
                                            .height(34.dp)
                                            .widthIn(max = 120.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text("Signature Area", fontSize = 7.5.sp, color = Color.LightGray)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = profile.signatureOwnerName?.ifBlank { "Authorized Signatory" } ?: "Authorized Signatory",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text("Consultant Pathologist, MD", fontSize = 7.5.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (!profile.reportFooterText.isNullOrBlank()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        Text(
                            text = profile.reportFooterText,
                            fontSize = 7.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
