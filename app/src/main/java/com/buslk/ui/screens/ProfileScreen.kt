package com.buslk.ui.screens

import androidx.compose.foundation.background
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
import com.buslk.data.Achievement
import com.buslk.data.Trip
import com.buslk.data.Feedback
import com.buslk.ui.auth.AuthViewModel
import com.buslk.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onSettingsClick: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser = (authUiState as? com.buslk.ui.auth.AuthUiState.Success)?.user
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Trip History", "Feedbacks", "Achievements")

    // We trigger the load even if uid is empty to show mock data for testing
    val uid = currentUser?.uid ?: ""
    LaunchedEffect(key1 = uid) {
        profileViewModel.loadProfileData(uid)
    }

    Scaffold(
        containerColor = Color(0xFFF5F6FA)
    ) { paddingValues ->
        when (uiState) {
            is ProfileUiState.Idle, is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BusLKBlue)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = (uiState as ProfileUiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            is ProfileUiState.Success -> {
                val successState = uiState as ProfileUiState.Success
                val userData = successState.userProfile
                val trips = successState.tripHistory
                val feedbacks = successState.feedbacks
                val achievementsMap = userData.achievements

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
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val displayName = userData.displayName.ifBlank { currentUser?.displayName ?: "Unknown User" }
                                            val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                                            Text(initials.ifBlank { "?" }, color = BusLKBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(userData.displayName.ifBlank { currentUser?.displayName ?: "Unknown User" }, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(userData.email.ifBlank { currentUser?.email ?: "" }, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldBadge
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.StarRate, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(userData.level.ifBlank { "Beginner" }, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                IconButton(onClick = onSettingsClick) {
                                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                                }
                            }
                        }
                    }

                    // --- 2. Overlapping Orange Points Card ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-40).dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(PointsOrangeStart, PointsOrangeEnd)
                                        )
                                    )
                                    .padding(horizontal = 24.dp, vertical = 20.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column {
                                            Text("Total Points", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                            Text("${userData.points}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                                        }
                                        Text("🏆", fontSize = 48.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val statModifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PointsCardInner)
                                            .padding(vertical = 16.dp)

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                            Text("${userData.stats.totalTrips}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Trips", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                            Text("${feedbacks.size}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Feedbacks", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                            val unlockedBadges = achievementsMap.values.count { it.unlocked }
                                            Text("$unlockedBadges", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Badges", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 3. Custom Tabs & Content ---
                    Column(modifier = Modifier.offset(y = (-24).dp).weight(1f)) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = Color.Black,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
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
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTabIndex) {
                            0 -> TripHistoryList(trips)
                            1 -> FeedbackList(feedbacks)
                            2 -> AchievementsGrid(achievementsMap)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripHistoryList(trips: List<Trip>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(trips) { trip ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(trip.route, fontWeight = FontWeight.Bold)
                        Text(trip.date, fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(trip.fare, color = BusLKBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun FeedbackList(feedbacks: List<Feedback>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(feedbacks) { feedback ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bus: ${feedback.busNo}", fontWeight = FontWeight.Bold)
                        Text("⭐ ${feedback.rating}", color = GoldBadge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(feedback.comment, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AchievementsGrid(achievements: Map<String, Achievement>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(achievements.values.toList()) { achievement ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (achievement.unlocked) Color.White else Color.LightGray.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                Text(if (achievement.unlocked) "🏆" else "🔒", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    achievement.name,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
