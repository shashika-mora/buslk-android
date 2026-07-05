package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    var reportCooldownSeconds by remember { mutableStateOf(0) }

    val uiState by tripViewModel.uiState.collectAsState()
    val checkedInState = uiState as? com.buslk.ui.viewmodels.TripUiState.CheckedIn
    val routeName = checkedInState?.routeName ?: "Loading Route..."
    val regNum = checkedInState?.regNum ?: busId
    val type = checkedInState?.type ?: "Standard"
    val capacity = checkedInState?.capacity ?: 40
    val owner = checkedInState?.owner ?: "Loading..."
    val overallRating = checkedInState?.overallRating ?: 4.5
    val recentComment = checkedInState?.recentComment ?: "No feedback yet."
    val recentUser = checkedInState?.recentUser ?: "User"
    val feedbacks = checkedInState?.feedbacks ?: emptyList()
    
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
                .verticalScroll(rememberScrollState())
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

            // --- 1.5 Bus Details Card ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Journey Bus", color = Color.Gray, fontSize = 12.sp)
                            Text(regNum, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BusLKBlue)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BusLKBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                type,
                                color = BusLKBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bus Owner / Operator", color = Color.Gray, fontSize = 11.sp)
                            Text(owner, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Seating Capacity", color = Color.Gray, fontSize = 11.sp)
                            Text("$capacity Seats", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
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

                    val scope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val handleReport: (String) -> Unit = { level ->
                            if (reportCooldownSeconds == 0) {
                                reportedCrowdLevel = level
                                tripViewModel.reportCrowdLevel(level)
                                
                                // Reset after 1 minute (60 seconds) with countdown
                                scope.launch {
                                    reportCooldownSeconds = 60
                                    while (reportCooldownSeconds > 0) {
                                        delay(1000)
                                        reportCooldownSeconds--
                                    }
                                    reportedCrowdLevel = null
                                }
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

                    // Cooldown / Success status text
                    if (reportCooldownSeconds > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("✓ Crowd level reported! Resubmit in ${reportCooldownSeconds}s", color = PositiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (reportedCrowdLevel != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("✓ Crowd level reported! +5 points", color = PositiveGreen, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 20.dp))

                    // --- Passenger Reviews & Ratings ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Passenger Reviews", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format(Locale.getDefault(), "%.1f / 5.0", overallRating), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (feedbacks.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No reviews submitted for this bus yet.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        feedbacks.forEach { feedback ->
                            val userMasked = feedback.userId.take(6).ifEmpty { "User12" }
                            val commentText = feedback.comment.ifEmpty { "Punctual bus, comfortable ride!" }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(50), color = BusLKBlue.copy(alpha = 0.1f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(userMasked.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "U", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BusLKBlue)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(userMasked, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("Verified Rider", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        commentText,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
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


