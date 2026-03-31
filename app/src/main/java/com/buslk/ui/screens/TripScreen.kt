package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.*
import com.buslk.ui.viewmodels.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    tripViewModel: TripViewModel,
    busId: String,
    onEndTrip: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reportedCrowdLevel by remember { mutableStateOf<String?>(null) }

    val uiState by tripViewModel.uiState.collectAsState()
    val routeName = (uiState as? com.buslk.ui.viewmodels.TripUiState.CheckedIn)?.routeName ?: "Loading Route..."
    val regNum = (uiState as? com.buslk.ui.viewmodels.TripUiState.CheckedIn)?.regNum ?: busId
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(routeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "Private Bus",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BusLKBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F6FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. Blue Status Header ---
            Surface(
                color = BusLKBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Current: Mount Lavinia", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("3 min", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Bar
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            thickness = 2.dp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(shape = RoundedCornerShape(50), color = Color.White, modifier = Modifier.size(10.dp)) {}
                            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(10.dp)) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Next: Dehiwala", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // --- 2. End Trip Button Card ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = UnreadRed.copy(alpha = 0.05f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            tripViewModel.endTrip()
                            onEndTrip()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UnreadRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚌", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Get Off / End Trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap when you're ready to end your journey",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // --- 3. Report & Help Section ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = BusLKBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report & Help", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Report Crowd Level:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val handleReport: (String) -> Unit = { level ->
                            if (reportedCrowdLevel == null) {
                                reportedCrowdLevel = level
                                tripViewModel.reportCrowdLevel(level)
                            }
                        }

                        CrowdReportButton(
                            label = "Low", 
                            icon = Icons.Outlined.Groups, 
                            isSelected = reportedCrowdLevel == "Low",
                            modifier = Modifier.weight(1f),
                            onClick = { handleReport("Low") }
                        )
                        CrowdReportButton(
                            label = "Medium", 
                            icon = Icons.Outlined.Groups, 
                            isSelected = reportedCrowdLevel == "Medium",
                            modifier = Modifier.weight(1f),
                            onClick = { handleReport("Medium") }
                        )
                        CrowdReportButton(
                            label = "High", 
                            icon = Icons.Outlined.Groups, 
                            isSelected = reportedCrowdLevel == "High",
                            modifier = Modifier.weight(1f),
                            onClick = { handleReport("High") }
                        )
                    }

                    if (reportedCrowdLevel != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("✓ Crowd level reported! +5 points", color = PositiveGreen, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { /* Report safety */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Safety Concern", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CrowdReportButton(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF1F2232) else Color.White
    val contentColor = if (isSelected) Color.White else Color.Gray

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        color = backgroundColor,
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}


