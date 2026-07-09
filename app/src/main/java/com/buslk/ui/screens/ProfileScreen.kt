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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.buslk.R
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
import com.buslk.ui.viewmodels.ProfileViewModel
import com.buslk.ui.viewmodels.ProfileUiState
import com.buslk.data.TripDoc
import com.buslk.data.FeedbackDoc
import com.buslk.data.AchievementDoc
import com.buslk.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

// Data classes removed as they are now sourced from com.buslk.data

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
    val tabs = listOf(
        stringResource(id = R.string.profile_tab_trip_history),
        stringResource(id = R.string.profile_tab_feedbacks),
        stringResource(id = R.string.profile_tab_achievements)
    )

    // Trigger data load when the screen becomes active
    val uid = currentUser?.uid ?: ""
    LaunchedEffect(key1 = uid) {
        if (uid.isNotBlank()) {
            profileViewModel.loadProfileData(uid)
        }
    }



    Scaffold(
        containerColor = Color(0xFFF5F6FA) // Light background
    ) { paddingValues ->
        when (uiState) {
            is ProfileUiState.Idle, is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BusLKBlue)
                }
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (uiState as ProfileUiState.Error).message, 
                        color = Color.Red, 
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { if (uid.isNotBlank()) profileViewModel.loadProfileData(uid) }) {
                        Text("Retry")
                    }
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
                    // Top User Header Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Info (Left aligned)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val fallbackName = stringResource(id = R.string.profile_unknown_user)
                                    val displayName = userData.displayName.ifBlank { currentUser?.displayName ?: fallbackName }
                                    val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                                    Text(initials.ifBlank { "?" }, color = BusLKBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                             }

                             Spacer(modifier = Modifier.width(12.dp))

                             Column {
                                 val fallbackName = stringResource(id = R.string.profile_unknown_user)
                                 Text(userData.displayName.ifBlank { currentUser?.displayName ?: fallbackName }, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                         val fallbackLevel = stringResource(id = R.string.profile_level_beginner)
                                         val displayLevel = if (userData.level.lowercase(Locale.getDefault()) == "beginner") {
                                             fallbackLevel
                                         } else {
                                             userData.level.ifBlank { fallbackLevel }
                                         }
                                         Text(displayLevel, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                     }
                                 }
                             }
                         }

                         // Settings Button
                         IconButton(onClick = onSettingsClick) {
                             Icon(Icons.Outlined.Settings, contentDescription = stringResource(id = R.string.settings_title), tint = Color.White)
                         }
                    }
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
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Column {
                            // Top Row: Points & Trophy Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(stringResource(id = R.string.profile_total_points), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                    Text("${userData.points}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                                }
                                
                                // Trophy Icon Placeholder
                                Text("🏆", fontSize = 48.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Bottom Row: 3 little stat cards
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
                                    Text(stringResource(id = R.string.profile_trips), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                    Text("${feedbacks.size}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(id = R.string.profile_feedbacks), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = statModifier) {
                                    val unlockedBadges = achievementsMap.values.count { it.unlocked }
                                    Text("$unlockedBadges", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(id = R.string.profile_badges), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Avoid layout shifting due to the offset above by offsetting the following content slightly up
            Column(modifier = Modifier.offset(y = (-24).dp).weight(1f)) {
                
                // --- 3. Custom Tabs ---
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
                                fontSize = 12.sp // Slightly smaller text to fit 3 tabs
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 4. Tab Content ---
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
fun TripHistoryList(trips: List<TripDoc>) {
    if (trips.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.profile_no_trips), color = Color.Gray)
        }
        return
    }

    // Formatter for timestamp
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(trips) { trip ->
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
                            // Extract just the route number (e.g. from "138-colombo-homagama" -> "138")
                            val routeNumber = trip.routeId.substringBefore("-").ifBlank { "N/A" }
                            
                            // Blue Route Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FoundBlue,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(routeNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Locations & Status
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(id = R.string.profile_bus_id_prefix, trip.busId), color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Status Pill
                                    val statusColor = when (trip.status.uppercase()) {
                                        "COMPLETED" -> PositiveGreen
                                        "ACTIVE", "ONGOING" -> BusLKBlue
                                        "CANCELLED" -> Color.Red
                                        else -> Color.Gray
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = statusColor.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = trip.status.uppercase(),
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val fallbackLoc = stringResource(id = R.string.profile_unknown)
                                    val startLoc = trip.startLocationName.ifBlank { fallbackLoc }
                                    val endLoc = trip.endLocationName.ifBlank { fallbackLoc }
                                    Text("$startLoc ➔ $endLoc", fontSize = 14.sp)
                                }
                            }
                        }
                        
                        // Points Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PositiveGreen
                        ) {
                            Text(
                                text = "+${trip.pointsEarned} pts",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Middle Metadata (Distance and Fare)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.profile_distance_fmt, trip.distanceKm), color = Color.DarkGray, fontSize = 12.sp)
                        Text(stringResource(id = R.string.profile_fare_fmt, trip.totalFare), color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Bottom Metadata (Date & Time)
                    val dateStr = trip.startTime?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                    val timeStr = trip.startTime?.toDate()?.let { timeFormat.format(it) } ?: "N/A"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateStr, color = Color.Gray, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕒", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(timeStr, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

// --- Master list of definitions for rendering ---
val achievementDefinitions = mapOf(
    "early_bird" to Triple("🌅", R.string.ach_early_bird_title, R.string.ach_early_bird_desc),
    "reporter" to Triple("📊", R.string.ach_reporter_title, R.string.ach_reporter_desc),
    "safety_first" to Triple("🛡️", R.string.ach_safety_first_title, R.string.ach_safety_first_desc),
    "explorer" to Triple("🗺️", R.string.ach_explorer_title, R.string.ach_explorer_desc),
    "eco_warrior" to Triple("🌱", R.string.ach_eco_warrior_title, R.string.ach_eco_warrior_desc),
    "helping_hand" to Triple("🤝", R.string.ach_helping_hand_title, R.string.ach_helping_hand_desc)
)

@Composable
fun AchievementsGrid(userAchievements: Map<String, AchievementDoc>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // We map over the static definitions to ensure all possible milestones are shown,
        // and pull their live progress from the `userAchievements` map.
        val itemsList = achievementDefinitions.entries.toList()
        
        items(itemsList) { (key, definition) ->
            val (emoji, titleRes, descRes) = definition
            val progressDoc = userAchievements[key]
            val isUnlocked = progressDoc?.unlocked == true

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
                    Text(emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Texts
                    Text(stringResource(id = titleRes), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(id = descRes), color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Status Pill
                    if (isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PositiveGreen
                        ) {
                            Text(stringResource(id = R.string.profile_unlocked), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    } else {
                        val progressDetails = if (progressDoc != null) "${progressDoc.progress}/${progressDoc.target}" else ""
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(stringResource(id = R.string.profile_locked_prefix, progressDetails), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackList(feedbacks: List<FeedbackDoc>) {
    if (feedbacks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.profile_no_feedbacks), color = Color.Gray)
        }
        return
    }

    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(feedbacks) { feedback ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    // Left Side: Overall Rating & Tags
                    Column(
                        modifier = Modifier.weight(0.3f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val overall = feedback.ratings["overall"] ?: 0
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(overall.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("/5", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        
                        // Stars
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (index < overall) Color(0xFFFFC107) else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        // Tags
                        if (feedback.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                feedback.tags.take(2).forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF5F5F5),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(tag.lowercase(), fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Right Side: Details, Comment, and Breakdown
                    Column(modifier = Modifier.weight(0.7f)) {
                        // Header (Bus ID & Route)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(feedback.busId.ifBlank { stringResource(id = R.string.profile_unknown_bus) }, color = Color.Gray, fontSize = 12.sp)
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("•", color = Color.LightGray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(feedback.routeId.ifBlank { stringResource(id = R.string.profile_general) }, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Date/Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val dateStr = feedback.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                            Text(dateStr, color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Comment Box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF9F9F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${feedback.comment.ifBlank { stringResource(id = R.string.profile_no_comment) }}\"",
                                fontSize = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Ratings Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val attributes = listOf(
                                "cleanliness" to stringResource(id = R.string.profile_cleanliness),
                                "comfort" to stringResource(id = R.string.profile_comfort),
                                "driver" to stringResource(id = R.string.profile_driver)
                            )
                            attributes.forEach { (key, label) ->
                                Column {
                                    Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text((feedback.ratings[key] ?: 0).toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}
