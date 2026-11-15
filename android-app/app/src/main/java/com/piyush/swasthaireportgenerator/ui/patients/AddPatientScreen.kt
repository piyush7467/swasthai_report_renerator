package com.piyush.swasthaireportgenerator.ui.patients

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.ui.components.SwasthAiTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    viewModel: PatientViewModel,
    onNavigateBack: () -> Unit,
    onPatientCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSalutation by remember { mutableStateOf("MR") }
    var salutationExpanded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }

    var selectedAgeUnit by remember { mutableStateOf("YEARS") }
    var ageUnitExpanded by remember { mutableStateOf(false) }

    var selectedGender by remember { mutableStateOf("MALE") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf<String?>(null) }
    val activeError = validationError ?: uiState.createError

    Scaffold(
        topBar = {
            SwasthAiTopAppBar(
                title = "Register New Patient",
                subtitle = "Patient demographic & contact records",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        enabled = !uiState.isCreating,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val age = ageStr.toIntOrNull()
                            if (name.isBlank()) {
                                validationError = "Patient full name is required."
                                return@Button
                            }
                            if (age == null || age <= 0 || age > 150) {
                                validationError = "Please enter a valid age between 1 and 150."
                                return@Button
                            }

                            val cleanedPhone = phone.trim().replace(Regex("[\\s-]"), "")
                            if (cleanedPhone.isNotEmpty() && !Regex("^\\+?[0-9]{7,15}$").matches(cleanedPhone)) {
                                validationError = "Phone number must be 7 to 15 digits (optional + prefix)."
                                return@Button
                            }

                            validationError = null
                            viewModel.createPatient(
                                salutation = selectedSalutation,
                                name = name.trim(),
                                age = age,
                                ageUnit = selectedAgeUnit,
                                gender = selectedGender,
                                phone = cleanedPhone.ifBlank { null },
                                email = email.trim().ifBlank { null },
                                address = address.trim().ifBlank { null }
                            ) {
                                onPatientCreated()
                            }
                        },
                        enabled = !uiState.isCreating,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp)
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Register Patient", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!activeError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = activeError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Personal Information Card
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
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Salutation Dropdown
                    ExposedDropdownMenuBox(
                        expanded = salutationExpanded,
                        onExpandedChange = { salutationExpanded = !salutationExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSalutation,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Salutation *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = salutationExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = salutationExpanded,
                            onDismissRequest = { salutationExpanded = false }
                        ) {
                            listOf("MR", "MRS", "MS", "MISS", "MASTER", "BABY", "DR").forEach { sal ->
                                DropdownMenuItem(
                                    text = { Text(sal) },
                                    onClick = {
                                        selectedSalutation = sal
                                        salutationExpanded = false
                                        when (sal) {
                                            "MR", "MASTER" -> selectedGender = "MALE"
                                            "MRS", "MS", "MISS" -> selectedGender = "FEMALE"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("Enter patient full name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Age + Unit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Age *") },
                            placeholder = { Text("e.g. 35") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Age Unit Dropdown
                        val ageUnitDisplay = when (selectedAgeUnit) {
                            "YEARS" -> "Years"
                            "MONTHS" -> "Months"
                            "DAYS" -> "Days"
                            else -> "Years"
                        }
                        ExposedDropdownMenuBox(
                            expanded = ageUnitExpanded,
                            onExpandedChange = { ageUnitExpanded = !ageUnitExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = ageUnitDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Age Unit *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ageUnitExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = ageUnitExpanded,
                                onDismissRequest = { ageUnitExpanded = false }
                            ) {
                                listOf("YEARS" to "Years", "MONTHS" to "Months", "DAYS" to "Days").forEach { (unitKey, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedAgeUnit = unitKey
                                            ageUnitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Gender Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gender *",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other").forEach { (genderKey, label) ->
                                val selected = selectedGender == genderKey
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedGender = genderKey },
                                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Contact & Location Card
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
                    Text(
                        text = "Contact & Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 9876543210") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("e.g. patient@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address (Optional)") },
                        placeholder = { Text("Enter street, city, pin code") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
