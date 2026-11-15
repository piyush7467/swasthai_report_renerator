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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
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
            // Header Badge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Patient & Health Data Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Last updated: November 2026 • Compliant with Indian Digital Personal Data Protection (DPDP) Act & International Healthcare Standards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Introduction
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.Info,
                    title = "1. Introduction",
                    content = "SwasthAI Lab (\"we\", \"our\", or \"the Platform\") is a diagnostic laboratory reporting and clinical workflow management solution operated for accredited diagnostic clinics, pathologists, and healthcare professionals. We are committed to protecting the privacy, confidentiality, and integrity of patient health records and personal data processed through our application."
                )
            }

            // Data We Collect
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.FactCheck,
                    title = "2. Information We Collect",
                    content = "To facilitate accurate diagnostic report generation, the app processes the following categories of data:\n\n" +
                            "• Patient Demographics: Full name, age, gender, contact number, and unique patient identifier (UHID/RefId).\n" +
                            "• Clinical & Diagnostic Parameters: Laboratory test names, measured bio-chemical values, units, reference range observations, and test notes.\n" +
                            "• Laboratory Staff & Facility Credentials: Name, email address, clinical designation (e.g., Pathologist, Lab Technician), digital signature image, and laboratory license number.\n" +
                            "• Diagnostic Reports: Generated PDF reports, QR code verification metadata, and certified report timestamps."
                )
            }

            // How We Use the Data
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.MedicalServices,
                    title = "3. How We Use Health Data",
                    content = "Collected data is used strictly for legitimate diagnostic reporting purposes:\n\n" +
                            "• Generating certified, standardized laboratory reports.\n" +
                            "• Providing patient test history for longitudinal clinical tracking.\n" +
                            "• Affixing authorized digital signatures and laboratory letterheads.\n" +
                            "• Authenticating laboratory personnel to ensure only authorized staff can approve and publish diagnostic reports.\n\n" +
                            "We DO NOT sell, rent, monetize, or disclose patient medical data to third-party advertisers or commercial brokers."
                )
            }

            // Security & Storage
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.Lock,
                    title = "4. Data Storage & Security",
                    content = "We employ industry-grade administrative, technical, and physical safeguards:\n\n" +
                            "• Encryption in Transit: All client-server communications use TLS 1.3 cryptographic protocols with HTTPS.\n" +
                            "• Token-Based Access Control: Secure JWT token authentication with role-based access restrictions (Admin, Pathologist, Technician).\n" +
                            "• Storage Isolation: Multi-tenant laboratory data isolation preventing unauthorized cross-clinic record access.\n" +
                            "• Certified Signatures: Tamper-evident digital signatures on generated PDF reports."
                )
            }

            // Data Retention & Deletion
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.DeleteForever,
                    title = "5. Account & Data Deletion Rights",
                    content = "In accordance with Google Play and Indus Appstore policies, you and your patients have full rights over personal data:\n\n" +
                            "• Deletion Request: Laboratory administrators can request complete account and data deletion at any time by contacting our Data Protection Officer.\n" +
                            "• Patient Record Removal: Individual patient profiles and unfinalized draft reports can be purged directly from the station database.\n" +
                            "• Contact for Deletion: To initiate a permanent data removal request, email support@swasthai.com with your registered laboratory credentials. Requests are fulfilled within 30 days in accordance with statutory medical record keeping requirements."
                )
            }

            // Third-Party Services
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.Hub,
                    title = "6. Sub-Processors & Infrastructure",
                    content = "Our backend services run on secured, compliant cloud server infrastructure. No advertising SDKs, tracking pixels, or third-party behavioral analytics trackers are bundled within this application."
                )
            }

            // Contact & Inquiries
            item {
                PolicySectionCard(
                    icon = Icons.Outlined.ContactSupport,
                    title = "7. Contact Data Protection Officer",
                    content = "If you have questions, grievances, or requests regarding this Privacy Policy or your healthcare records, please contact:\n\n" +
                            "SwasthAI Compliance & Privacy Desk\n" +
                            "Email: privacy@swasthai.com\n" +
                            "Support: support@swasthai.com\n" +
                            "Operating Hours: Monday – Saturday, 9:00 AM – 6:00 PM IST"
                )
            }
        }
    }
}

@Composable
private fun PolicySectionCard(
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
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

