package com.piyush.swasthaireportgenerator.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StepInfo(
    val step: Int,
    val title: String,
    val subtitle: String
)

val WORKFLOW_STEPS = listOf(
    StepInfo(1, "Patient", "Select / Register"),
    StepInfo(2, "Doctor", "Assign Pathologist"),
    StepInfo(3, "Select Tests", "Lab Catalog"),
    StepInfo(4, "Enter Results", "Parameters & Flags"),
    StepInfo(5, "Review & Finalize", "Seal & Letterhead")
)

@Composable
fun ReportWorkflowStepper(
    currentStep: Int,
    maxAccessibleStep: Int,
    onSelectStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with current step name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$currentStep",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "STEP $currentStep OF 5",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = WORKFLOW_STEPS[currentStep - 1].title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = WORKFLOW_STEPS[currentStep - 1].subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Stepper indicator dots & connecting line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WORKFLOW_STEPS.forEachIndexed { index, stepItem ->
                    val isCompleted = stepItem.step < currentStep
                    val isCurrent = stepItem.step == currentStep
                    val isClickable = stepItem.step <= maxAccessibleStep

                    // Step Circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isCompleted -> Color(0xFF059669) // Emerald
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .then(
                                if (isCurrent) Modifier.border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                else Modifier
                            )
                            .clickable(enabled = isClickable) { onSelectStep(stepItem.step) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${stepItem.step}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Connecting Line
                    if (index < WORKFLOW_STEPS.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (stepItem.step < currentStep) Color(0xFF059669) else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
        }
    }
}
