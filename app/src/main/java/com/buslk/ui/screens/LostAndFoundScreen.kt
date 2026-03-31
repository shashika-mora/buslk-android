package com.buslk.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.*

// --- Data Model ---
enum class ItemType { LOST, FOUND }

data class LostFoundItemV2(
    val id: String,
    val title: String,
    val description: String,
    val type: ItemType,
    val route: String,
    val location: String,
    val timePosted: String,
    val reporterName: String,
    val isClosed: Boolean = false
)

val mockItemsV2 = listOf(
    LostFoundItemV2(
        id = "1", title = "Black Backpack", description = "Small black backpack with laptop inside. Found near seat 12A.", type = ItemType.FOUND, route = "Route 138", location = "Seat 12A", timePosted = "15 min ago", reporterName = "Amal P."
    ),
    LostFoundItemV2(
        id = "2", title = "Phone Charger", description = "White iPhone charger cable with adapter", type = ItemType.LOST, route = "Route 176", location = "Back seat area", timePosted = "1 hour ago", reporterName = "Priya S."
    ),
    LostFoundItemV2(
        id = "3", title = "Water Bottle", description = "Blue metal water bottle with university stickers", type = ItemType.FOUND, route = "Route 138", location = "Front rows", timePosted = "3 hours ago", reporterName = "Kamal F.", isClosed = true
    ),
    LostFoundItemV2(
        id = "4", title = "Umbrella", description = "Black folding umbrella, brand new", type = ItemType.LOST, route = "Route 120", location = "Luggage rack", timePosted = "Yesterday", reporterName = "Saman D."
    )
)

// Language Support (Simple Mock)
enum class AppLanguage { EN, SI, TA }
var currentLanguage by mutableStateOf(AppLanguage.EN)

fun getLocalizedString(en: String, si: String, ta: String): String {
    return when(currentLanguage) {
        AppLanguage.EN -> en
        AppLanguage.SI -> si
        AppLanguage.TA -> ta
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf("All Routes") }
    val routes = listOf("All Routes", "Route 138", "Route 176", "Route 120", "Route 177")
    
    // Tab Index: 0 = All, 1 = Found, 2 = Lost
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Filtering Logic
    val filteredItems = mockItemsV2.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) || 
                            item.description.contains(searchQuery, ignoreCase = true)
        val matchesRoute = selectedRoute == "All Routes" || item.route == selectedRoute
        val matchesTab = when (selectedTabIndex) {
            1 -> item.type == ItemType.FOUND
            2 -> item.type == ItemType.LOST
            else -> true
        }
        matchesSearch && matchesRoute && matchesTab
    }
    
    // Tab titles with counts
    val allCount = mockItemsV2.filter { (selectedRoute == "All Routes" || it.route == selectedRoute) && (it.title.contains(searchQuery, true) || it.description.contains(searchQuery, true)) }.size
    val foundCount = mockItemsV2.filter { it.type == ItemType.FOUND && (selectedRoute == "All Routes" || it.route == selectedRoute) && (it.title.contains(searchQuery, true) || it.description.contains(searchQuery, true)) }.size
    val lostCount = mockItemsV2.filter { it.type == ItemType.LOST && (selectedRoute == "All Routes" || it.route == selectedRoute) && (it.title.contains(searchQuery, true) || it.description.contains(searchQuery, true)) }.size
    
    val tabs = listOf(
        getLocalizedString("All ($allCount)", "සියල්ල ($allCount)", "அனைத்தும் ($allCount)"),
        getLocalizedString("Found ($foundCount)", "හමුවූ ($foundCount)", "கண்டுபிடிக்கப்பட்டது ($foundCount)"),
        getLocalizedString("Lost ($lostCount)", "නැතිවූ ($lostCount)", "தொலைந்தது ($lostCount)")
    )

    Scaffold(
        containerColor = LightGrayBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. Top Header & 2. Search Bar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF310065), FriendsPurple) // Dark purple to light purple
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    // Top Row: Back, Title, Language toggle (optional extra)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Back to previous screen */ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Icon(Icons.Outlined.Inventory2, contentDescription = "Cube Icon", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getLocalizedString("Lost & Found", "නැතිවූ සහ හමුවූ", "இழந்த மற்றும் கண்டறியப்பட்ட"),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Language toggle for demonstration
                        Text(
                            text = currentLanguage.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    currentLanguage = when (currentLanguage) {
                                        AppLanguage.EN -> AppLanguage.SI
                                        AppLanguage.SI -> AppLanguage.TA
                                        AppLanguage.TA -> AppLanguage.EN
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(getLocalizedString("Search items...", "සොයන්න...", "தேடு..."), color = Color(0x99FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x33FFFFFF), // Light purple bg (transparent white over purple)
                            unfocusedContainerColor = Color(0x33FFFFFF),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. Route Filters ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Icon Start
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = FriendsPurple,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                
                routes.forEach { route ->
                    val isSelected = route == selectedRoute
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color.White else Color(0xFFE8DBFA), // Light purple for unselected
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .clickable { selectedRoute = route }
                            .animateContentSize()
                    ) {
                        Text(
                            text = route,
                            color = if (isSelected) FriendsPurple else Color(0xFF6B4B8E),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // --- 5. Tabs Section ---
            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    color = if (isSelected) FriendsPurple else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedTabIndex = index }
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. Post New Item Button (Moved inside scrolling area or sticky above list) ---
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { /* Open form screen */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        getLocalizedString("Post New Item", "නව අයිතමය එකතු කරන්න", "புதிய உருப்படியை இடுகையிடவும்"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 6. Content List ---
            AnimatedContent(
                targetState = filteredItems.isEmpty(),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "ListAnimation"
            ) { isEmpty ->
                if (isEmpty) {
                    // Empty State
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "No Items",
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            getLocalizedString("No items found", "අයිතම හමුවූයේ නැත", "உருப்படிகள் எதுவும் இல்லை"),
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ItemCardV2(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCardV2(item: LostFoundItemV2) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp, // Modern card-based soft shadow
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to detail */ }
            .animateContentSize() // Subtle animation
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type Badge
                    val isFound = item.type == ItemType.FOUND
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFound) FoundBlue.copy(alpha = 0.15f) else LostOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isFound) getLocalizedString("Found", "හමුවූ", "கண்டது") else getLocalizedString("Lost", "නැතිවූ", "இழந்தது"),
                            color = if (isFound) FoundBlue else LostOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Route Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF3F4F6) // Light grey
                    ) {
                        Text(
                            text = item.route,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Open/Claimed Button/Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isClosed) Color(0xFFE0E0E0) else OpenGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (item.isClosed) getLocalizedString("Closed", "වසා ඇත", "மூடப்பட்டது") else getLocalizedString("Open", "විවෘතයි", "திறந்துள்ளது"),
                        color = if (item.isClosed) Color.Gray else OpenGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Description
            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details Row (Location & Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn, 
                    contentDescription = null, 
                    tint = FriendsPurple, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.location, 
                    color = Color.DarkGray, 
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "•", 
                    color = Color.LightGray, 
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = item.timePosted, 
                    color = Color.Gray, 
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small Avatar Placeholder
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = Color(0xFFE8DBFA) // Light purple
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.reporterName.firstOrNull()?.toString() ?: "?",
                                color = FriendsPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${getLocalizedString("Reported by", "වාර්තා කළේ", "அறிவித்தது")} ${item.reporterName}",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                if (!item.isClosed) {
                    Button(
                        onClick = { /* Contact action */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FriendsPurple
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (item.type == ItemType.FOUND) getLocalizedString("Contact", "අමතන්න", "தொடர்பு கொள்ள") 
                                   else getLocalizedString("I Found This!", "මට හමුවුණා!", "நான் இதை கண்டுபிடித்தேன்!"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}