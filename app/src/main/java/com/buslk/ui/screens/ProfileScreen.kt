package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.auth.AuthViewModel
import com.buslk.ui.theme.*

// --- Mock Data ---
data class InternalTripHistory(
    val id: String,
    val route: String,
    val busType: String,
    val startLoc: String,
    val endLoc: String,
    val points: Int,
    val date: String,
    val time: String
)

val mockTrips = listOf(
    InternalTripHistory("1", "138", "Private Bus", "Colombo Fort", "Mount Lavinia", 25, "Feb 9, 2026", "08:30 AM"),
    InternalTripHistory("2", "176", "Government Bus", "Pettah", "Dehiwala", 30, "Feb 8, 2026", "05:15 PM")
)

data class AchievementItem(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val unlocked: Boolean
)

val mockAchievements = listOf(
    AchievementItem("1", "🌅", "Early Bird", "10 morning trips", true),
    AchievementItem("2", "📊", "Reporter", "20 crowd reports", true),
    AchievementItem("3", "🛡️", "Safety First", "5 safety reports", true),
    AchievementItem("4", "🗺️", "Explorer", "25 different routes", false),
    AchievementItem("5", "🌱", "Eco Warrior", "50 bus trips", true),
    AchievementItem("6", "🤝", "Helping Hand", "10 lost items found", false)
)

data class FeedbackItem(
    val id: String,
    val title: String,
    val comment: String,
    val date: String
)

val mockFeedbacks = listOf(
    FeedbackItem("1", "Driver was very polite", "Route 138 - Appreciated the safe driving.", "Feb 5, 2026"),
    FeedbackItem("2", "Bus was too crowded", "Route 170 - More buses needed at 5 PM.", "Feb 1, 2026")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Trip History", "Feedbacks", "Achievements")

    // Assuming user details are fetched from ViewModel, using hardcoded for UI mockup
    val userName = "Amal Perera"
    val userEmail = "amal.perera@email.com"

    Scaffold(
        containerColor = Color(0xFFF5F6FA) // Light background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. Blue Header ---
            Surface(
                color = BusLKBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Top App Bar Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        // Settings / Logout Button
                        IconButton(onClick = {
                            authViewModel.signOut()
                            onLogoutSuccess()
                        }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings / Logout", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar Circle
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("AP", color = BusLKBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(userName, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(userEmail, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Gold Member Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldBadge
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.StarRate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gold Member", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp)) // Extra padding before the overlapping card
                }
            }

            // --- 2. Overlapping Orange Points Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp) // Pull it up over the blue header
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Gradient Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(PointsOrangeStart, PointsOrangeEnd)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            // Top Row: Points & Trophy Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text("Total Points", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                    Text("1450", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                                }
                                
                                // Trophy Icon Placeholder
                                Text("🏆", fontSize = 48.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Bottom Row: 3 little stat cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val statModifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PointsCardInner)
                                    .padding(vertical = 12.dp)

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                    Text("47", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Trips", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                    Text("23", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Reports", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                    Text("4", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Badges", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Avoid layout shifting due to the offset above by offsetting the following content slightly up
            Column(modifier = Modifier.offset(y = (-24).dp)) {
                
                // --- 3. Custom Tabs ---
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color.Transparent
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = selectedTabIndex == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .background(if (selected) Color.White else Color.Transparent)
                                .clip(RoundedCornerShape(50))
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp // Slightly smaller text to fit 3 tabs
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 4. Tab Content ---
                when (selectedTabIndex) {
                    0 -> TripHistoryList()
                    1 -> FeedbackList()
                    2 -> AchievementsGrid()
                }
            }
        }
    }
}

@Composable
fun TripHistoryList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mockTrips) { trip ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            // Blue Route Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FoundBlue,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(trip.route, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Locations
                            Column {
                                Text(trip.busType, color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${trip.startLoc} ➔ ${trip.endLoc}", fontSize = 14.sp)
                                }
                            }
                        }
                        
                        // Points Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PositiveGreen
                        ) {
                            Text(
                                text = "+${trip.points} pts",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(trip.date, color = Color.Gray, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕒", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(trip.time, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
fun AchievementsGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mockAchievements) { achievement ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.85f) // Slightly taller than wide
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Emoji / Icon
                    Text(achievement.emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Texts
                    Text(achievement.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(achievement.desc, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Status Pill
                    if (achievement.unlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PositiveGreen
                        ) {
                            Text("🏆 Unlocked", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("Locked", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
fun FeedbackList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mockFeedbacks) { feedback ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(feedback.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(feedback.comment, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(feedback.date, color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}
