package com.piyush.swasthaireportgenerator.ui.patients

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddPatientDialog(
    onDismissRequest: () -> Unit,
    onSavePatient: (
        salutation: String,
        name: String,
        age: Int,
        ageUnit: String,
        gender: String,
        phone: String?,
        email: String?,
        address: String?
    ) -> Unit,
    isSaving: Boolean,
    serverError: String? = null
) {
    AddPatientDialogInternal(
        dialogTitle = "Register New Patient",
        confirmLabel = "Register",
        initialSalutation = "MR",
        initialName = "",
        initialAge = "",
        initialAgeUnit = "YEARS",
        initialGender = "MALE",
        initialPhone = "",
        initialEmail = "",
        initialAddress = "",
        onDismissRequest = onDismissRequest,
        onSavePatient = onSavePatient,
        isSaving = isSaving,
        serverError = serverError
    )
}

@Composable
internal fun AddPatientDialogInternal(
    dialogTitle: String,
    confirmLabel: String,
    initialSalutation: String,
    initialName: String,
    initialAge: String,
    initialAgeUnit: String,
    initialGender: String,
    initialPhone: String,
    initialEmail: String,
    initialAddress: String,
    onDismissRequest: () -> Unit,
    onSavePatient: (
        salutation: String,
        name: String,
        age: Int,
        ageUnit: String,
        gender: String,
        phone: String?,
        email: String?,
        address: String?
    ) -> Unit,
    isSaving: Boolean,
    serverError: String? = null
) {
    var selectedSalutation by remember(initialSalutation) { mutableStateOf(initialSalutation) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var ageStr by remember(initialAge) { mutableStateOf(initialAge) }
    var selectedAgeUnit by remember(initialAgeUnit) { mutableStateOf(initialAgeUnit) }
    var selectedGender by remember(initialGender) { mutableStateOf(initialGender) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var address by remember(initialAddress) { mutableStateOf(initialAddress) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val activeError = validationError ?: serverError

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismissRequest()
        },
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!activeError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Salutation
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Salutation *",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("MR", "MRS", "MS", "MISS", "MASTER", "BABY", "DR").forEach { sal ->
                            FilterChip(
                                selected = selectedSalutation == sal,
                                onClick = {
                                    selectedSalutation = sal
                                    when (sal) {
                                        "MR", "MASTER" -> selectedGender = "MALE"
                                        "MRS", "MS", "MISS" -> selectedGender = "FEMALE"
                                    }
                                },
                                label = { Text(sal) }
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Age + Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Age *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1.6f)) {
                        Text(
                            text = "Age Unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("YEARS" to "Yrs", "MONTHS" to "Mos", "DAYS" to "Days").forEach { (unitKey, label) ->
                                FilterChip(
                                    selected = selectedAgeUnit == unitKey,
                                    onClick = { selectedAgeUnit = unitKey },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // Gender Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Gender *",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other").forEach { (genderKey, label) ->
                            FilterChip(
                                selected = selectedGender == genderKey,
                                onClick = { selectedGender = genderKey },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Phone (Optional, validated if provided)
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

                // Email (Optional)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Address (Optional)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                    onSavePatient(
                        selectedSalutation,
                        name.trim(),
                        age,
                        selectedAgeUnit,
                        selectedGender,
                        cleanedPhone.ifBlank { null },
                        email.trim().ifBlank { null },
                        address.trim().ifBlank { null }
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}
