package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buslk.ui.theme.BusLKTheme
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    user: FirebaseUser?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Extracted colors based on the design
    val headerBlue = Color(0xFF1E5FF5)
    val statsOrange = Color(0xFFF99B00)
    val bgGray = Color(0xFFF5F7FB)
    
    when (val state = uiState) {
        is ProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize().background(bgGray), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = headerBlue)
            }
        }
        is ProfileUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().background(bgGray), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = Color.Red)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadProfileData() }) { Text("Retry") }
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = onSignOut) { Text("Sign Out Instead") }
                }
            }
        }
        is ProfileUiState.Success -> {
            val userDoc = state.userDoc
            val trips = state.trips
            val achievements = state.achievements

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgGray)
            ) {
                item {
                    // Top Header Section (Blue Background)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBlue)
                            .padding(bottom = 50.dp) // Extra padding for overlapping card
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Top Action Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Profile",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = onSignOut) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // User Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getInitials(userDoc.displayName.ifEmpty { user?.displayName ?: "User" }),
                                        color = headerBlue,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = userDoc.displayName.ifEmpty { user?.displayName ?: "User" },
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = userDoc.email.ifEmpty { user?.email ?: "" },
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Membership Level Badge
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFFFB703),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = userDoc.level,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                item {
                    // Stats Card (Overlapping the blue header)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-40).dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = statsOrange,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Points",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = userDoc.points.toString(),
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = "Trophy",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Sub-stats breakdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatBox(value = userDoc.stats.totalTrips.toString(), label = "Trips", modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatBox(value = userDoc.stats.reportsSubmitted.toString(), label = "Reports", modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatBox(value = achievements.count { it.isUnlocked }.toString(), label = "Badges", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                item {
                    // Custom Tab Row Wrapper
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-20).dp)
                    ) {
                        // Segmented Button / Custom Tabs
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFE9EDF5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TabItem(
                                    text = "Trip History",
                                    isSelected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                TabItem(
                                    text = "Achievements",
                                    isSelected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Tab Content
                        if (selectedTabIndex == 0) {
                            TripHistoryTabContent(trips)
                        } else {
                            AchievementsTabContent(achievements)
                        }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Settings & Preferences
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { /* Handle Settings */ }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Settings & Preferences",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "Go",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
        }
    }
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.2f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color.White else Color.Transparent
    val contentColor = if (isSelected) Color.Black else Color.Gray

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TripHistoryTabContent(trips: List<com.buslk.data.TripDoc>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (trips.isEmpty()) {
            Text(
                "No trips found.",
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            trips.forEach { trip ->
                TripHistoryItem(trip)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // View All Trips Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                    .clickable { /* Handle View All Trips */ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All Trips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "View All",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TripHistoryItem(trip: com.buslk.data.TripDoc) {
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateStr = trip.timestamp?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
    val timeStr = trip.timestamp?.toDate()?.let { timeFormatter.format(it) } ?: "N/A"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Bus Number Box & Details
                Row {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E5FF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trip.busNumber.ifEmpty { "BUS" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = trip.type,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trip.startLocation} → ${trip.destination}",
                                color = Color.DarkGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Points Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00B14F)
                ) {
                    Text(
                        text = "+${trip.points} pts",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            HorizontalDivider(
                color = Color(0xFFF0F0F0),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            // Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = dateStr, color = Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeStr, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}


@Composable
fun AchievementsTabContent(achievements: List<AchievementUI>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (achievements.isEmpty()) {
            Text(
                "No achievements available.",
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            for (i in achievements.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AchievementItem(achievements[i])
                    }
                    if (i + 1 < achievements.size) {
                        Box(modifier = Modifier.weight(1f)) {
                            AchievementItem(achievements[i + 1])
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: AchievementUI) {
    val alphaColor = if (achievement.isUnlocked) 1f else 0.5f

    // Resolve icon from iconName dynamically if possible, falling back to star
    val iconVector = when(achievement.iconName.lowercase()) {
        "wb_sunny", "earlybird" -> Icons.Outlined.WbSunny
        "assessment", "reporter" -> Icons.Outlined.Assessment
        "security", "safety" -> Icons.Outlined.Security
        "map", "explorer" -> Icons.Outlined.Map
        "eco", "ecowarrior" -> Icons.Outlined.Eco
        "handshake", "helpinghand" -> Icons.Outlined.Handshake
        else -> Icons.Outlined.Star
    }

    val iconColor = if (achievement.isUnlocked) {
        when(achievement.iconName.lowercase()) {
            "wb_sunny", "earlybird" -> Color(0xFFFFA726)
            "assessment", "reporter" -> Color(0xFFAB47BC)
            "security", "safety" -> Color(0xFF42A5F5)
            "eco", "ecowarrior" -> Color(0xFF66BB6A)
            else -> Color(0xFF1E5FF5)
        }
    } else Color.LightGray

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFF), // Very soft blue-white background
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height to make nice squares
            .border(1.dp, Color(0xFFE4E9FA), RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Centered Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .padding(bottom = 20.dp) // Push up slightly to leave room for the badge
            ) {
                // Background circle for icon if needed, or just the icon
                Icon(
                    imageVector = iconVector,
                    contentDescription = achievement.title,
                    tint = iconColor.copy(alpha = alphaColor),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (achievement.isUnlocked) Color.Black else Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = achievement.description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            // Badge in Bottom-Left Corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                if (achievement.isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFF00B14F)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MilitaryTech,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unlocked",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Text(
                            text = "Locked",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    if (parts.isEmpty()) return "U"
    if (parts.size == 1) return parts[0].take(1).uppercase()
    return "${parts[0].take(1)}${parts.last().take(1)}".uppercase()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    BusLKTheme {
        ProfileScreen(user = null, onSignOut = {})
    }
}
