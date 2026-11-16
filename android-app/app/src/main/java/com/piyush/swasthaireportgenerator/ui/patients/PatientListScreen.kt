package com.piyush.swasthaireportgenerator.ui.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.data.model.PatientResponse
import com.piyush.swasthaireportgenerator.ui.components.SkeletonPatientList

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    viewModel: PatientViewModel,
    onOpenDrawer: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onPatientClick: (PatientResponse) -> Unit,
    onNewTestClick: (PatientResponse) -> Unit = onPatientClick
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.patients.isEmpty() && !uiState.isLoading) {
            viewModel.loadPatients()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Text(
                        text = "Patient Directory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Sidebar Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Patient Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Directory") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.loadPatients(searchQuery.ifBlank { null })
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Search") },
                                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                                enabled = searchQuery.isNotBlank(),
                                onClick = {
                                    showOverflowMenu = false
                                    searchQuery = ""
                                    viewModel.loadPatients(null)
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
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadPatients(searchQuery.ifBlank { null }) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp)
            ) {

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.loadPatients(it.ifBlank { null })
                },
                placeholder = { Text("Search by patient name, UHID, or phone...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.loadPatients(null)
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.isLoading && uiState.patients.isEmpty()) {
                SkeletonPatientList()
            } else if (uiState.patients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Empty",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Patients Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ New Patient' to register a patient for diagnostic testing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(uiState.patients) { patient ->
                        PatientCard(
                            patient = patient,
                            onCardClick = { onPatientClick(patient) },
                            onNewTestClick = { onNewTestClick(patient) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun PatientCard(
    patient: PatientResponse,
    onCardClick: () -> Unit,
    onNewTestClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Initials Avatar
            Surface(
                modifier = Modifier.size(46.dp),
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Details
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${patient.salutation ?: ""} ${patient.name}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "UHID: ${patient.patientCode} • ${patient.ageValue ?: 0} ${patient.ageUnit ?: "Yrs"} • ${patient.gender ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!patient.phone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = patient.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick "+ Test" Action
            OutlinedButton(
                onClick = onNewTestClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Test", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
