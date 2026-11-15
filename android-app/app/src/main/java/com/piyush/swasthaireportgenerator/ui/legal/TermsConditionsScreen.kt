package com.piyush.swasthaireportgenerator.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Critical Medical Disclaimer Callout
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Important Medical Disclaimer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "SwasthAI Lab is a clinical data management and automated diagnostic reporting software suite designed solely for use by certified laboratories, pathologists, and medical practitioners.\n\n" +
                                    "This application does NOT offer independent medical advice, clinical diagnosis, or prescriptive treatment. All laboratory findings and generated reports MUST be reviewed, validated, and signed by a licensed pathologist or qualified medical authority.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Acceptance
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.Gavel,
                    title = "1. Agreement to Terms",
                    content = "By accessing, installing, or using the SwasthAI Lab mobile application or associated web portals, you represent and warrant that you are authorized by your diagnostic laboratory or medical institution and agree to be bound by these Terms and Conditions."
                )
            }

            // Permitted Use & Authorized Users
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.VerifiedUser,
                    title = "2. Authorized Personnel & Usage",
                    content = "• The application is restricted to verified healthcare professionals, pathologists, biochemists, and lab technicians.\n" +
                            "• Users agree to maintain strict confidentiality of laboratory credentials, station access pins, and two-factor authentication tokens.\n" +
                            "• Any unauthorized sharing of account credentials that compromises patient privacy is strictly prohibited."
                )
            }

            // Clinical Accuracy & Laboratory Responsibility
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.Biotech,
                    title = "3. Laboratory & Diagnostic Responsibility",
                    content = "• The laboratory and its designated signatory bear sole professional responsibility for the accuracy of recorded biochemical and clinical test metrics.\n" +
                            "• Automated reference ranges and flagged abnormal values provided by SwasthAI are guideline markers and must be verified against your laboratory's localized instrument calibration standards.\n" +
                            "• Affixing a digital signature certifies that authorized staff have reviewed the diagnostic findings."
                )
            }

            // Data Ownership
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.Description,
                    title = "4. Patient Records & Data Ownership",
                    content = "Your laboratory retains full ownership and control over all patient demographics, test results, and customized letterhead assets uploaded to the station. SwasthAI processes this data strictly as a technical service provider."
                )
            }

            // Software Availability & Liability
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.CloudDone,
                    title = "5. Service Availability & Limitation of Liability",
                    content = "While we strive for 99.9% uptime, SwasthAI shall not be liable for clinical delays, temporary network outages, or hardware failures occurring at local laboratory stations. Laboratories should maintain standard operational contingency procedures."
                )
            }

            // Changes & Termination
            item {
                TermsSectionCard(
                    icon = Icons.Outlined.Update,
                    title = "6. Modifications & Inquiries",
                    content = "We reserve the right to update these terms to reflect evolving healthcare regulatory requirements. Continued use constitutes acceptance.\n\n" +
                            "For legal inquiries:\n" +
                            "Email: legal@swasthai.com\n" +
                            "Address: SwasthAI Healthcare Technologies Private Limited"
                )
            }
        }
    }
}

@Composable
private fun TermsSectionCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

