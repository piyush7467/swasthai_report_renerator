package com.piyush.swasthaireportgenerator.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyush.swasthaireportgenerator.R
import com.piyush.swasthaireportgenerator.ui.navigation.BottomBarTab

@Composable
fun AppDrawerContent(
    currentTab: BottomBarTab,
    userName: String,
    userRole: String,
    userEmail: String,
    organizationName: String,
    logoBytes: ByteArray? = null,
    onTabSelected: (BottomBarTab) -> Unit,
    onNavigateToTestCatalog: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToOrgSettings: () -> Unit = {},
    onOpenThemeDialog: () -> Unit,
    onLogoutClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Professional Laboratory Station Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F766E), // Deep Teal
                                Color(0xFF115E59)  // Darker Teal
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo in rounded white container
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            val logoBitmap = remember(logoBytes) {
                                logoBytes?.let { bytes ->
                                    try {
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = "Laboratory Logo",
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_swasthai_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // Station Status Pill (Green dot)
                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                                Text(
                                    text = "Station Online",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = organizationName.ifBlank { "SwasthAI Laboratory" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Clinical Diagnostic & Pathology System",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCFBF1)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                    // Staff Profile Snippet
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = userName.split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                Text(
                                    text = initials.ifBlank { "S" },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = userName.ifBlank { "Laboratory Staff" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = userRole,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (userEmail.isNotBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Navigation Items
            Text(
                text = "CLINICAL WORKSPACE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (currentTab == BottomBarTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                        contentDescription = "Dashboard"
                    )
                },
                label = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == BottomBarTab.DASHBOARD,
                onClick = {
                    onTabSelected(BottomBarTab.DASHBOARD)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (currentTab == BottomBarTab.PATIENTS) Icons.Filled.People else Icons.Outlined.People,
                        contentDescription = "Patients"
                    )
                },
                label = { Text("Patient Directory", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == BottomBarTab.PATIENTS,
                onClick = {
                    onTabSelected(BottomBarTab.PATIENTS)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (currentTab == BottomBarTab.REPORTS) Icons.AutoMirrored.Filled.Assignment else Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = "Reports"
                    )
                },
                label = { Text("Reports & Worklist", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == BottomBarTab.REPORTS,
                onClick = {
                    onTabSelected(BottomBarTab.REPORTS)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Biotech,
                        contentDescription = "Catalog",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = { Text("Test Catalog & Ranges", fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToTestCatalog()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

            // 2b. Organization Administration
            Text(
                text = "ORGANIZATION & LICENSING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (userRole.equals("ORG_ADMIN", ignoreCase = true)) {
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Staff Management"
                        )
                    },
                    label = { Text("Lab Staff Management", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onNavigateToStaff()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Plans & Subscription"
                    )
                },
                label = { Text("Plans & Subscription", fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToLicense()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Lab Profile & Branding"
                    )
                },
                label = { Text("Lab Profile & Branding", fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigateToOrgSettings()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

            // 3. System & Preferences
            Text(
                text = "SYSTEM & PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = "Themes") },
                label = { Text("Customize Appearance", fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenThemeDialog()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (currentTab == BottomBarTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                },
                label = { Text("Settings & Connection", fontWeight = FontWeight.SemiBold) },
                selected = currentTab == BottomBarTab.SETTINGS,
                onClick = {
                    onTabSelected(BottomBarTab.SETTINGS)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. Footer & Logout
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onLogoutClick()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SwasthAI Diagnostic Engine v1.0.0\nSSL 256-Bit Encrypted • HIPAA Aligned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
