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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
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

// --- Mock Data ---
data class FriendState(
    val name: String,
    val initials: String,
    val statusText: String,
    val isOnline: Boolean,
    val unreadCount: Int = 0,
    val isOnBus: Boolean = false
)

val mockOnlineFriends = listOf(
    FriendState("Nimal Perera", "NP", "Active now", true, 1),
    FriendState("Maya Rajapaksa", "MR", "Active 5m ago", true, 0)
)

val mockOtherBusesFriends = listOf(
    FriendState("Priya Silva", "PS", "On Route 138", true, 2, isOnBus = true),
    FriendState("Kamal Fernando", "KF", "On Route 138", true, 0, isOnBus = true),
    FriendState("Saman De Silva", "SD", "Active 10m ago", true, 0, isOnBus = true)
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onChatClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(1) } // Default to "Online"
    
    // Tab calculations
    val onlineCount = mockOnlineFriends.filter { it.name.contains(searchQuery, true) }.size
    val allCount = (mockOnlineFriends + mockOtherBusesFriends).filter { it.name.contains(searchQuery, true) }.size
    
    val tabs = listOf(
        getLocalizedString("On Bus (0)", "බස් රථයේ (0)", "பேருந்தில் (0)"),
        getLocalizedString("Online ($onlineCount)", "මාර්ගගත ($onlineCount)", "ஆன்லைன் ($onlineCount)"),
        getLocalizedString("All ($allCount)", "සියල්ල ($allCount)", "அனைத்தும் ($allCount)")
    )

    Scaffold(
        containerColor = LightGrayBg,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add Friend */ },
                shape = CircleShape,
                containerColor = OpenGreen,
                contentColor = Color.White,
                modifier = Modifier.shadow(4.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Friend")
            }
        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Friends Icon", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getLocalizedString("Friends", "යහළුවන්", "நண்பர்கள்"),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Language toggle indicator
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
                        placeholder = { Text(getLocalizedString("Search friends...", "යහළුවන් සොයන්න...", "நண்பர்களைத் தேடு..."), color = Color(0x99FFFFFF)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x33FFFFFF),
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
            
            // --- 3. Tabs Section ---
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
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. Content List ---
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "ListAnimation"
            ) { tabIndex ->
                
                val filteredOnline = mockOnlineFriends.filter { it.name.contains(searchQuery, true) }
                val filteredOther = mockOtherBusesFriends.filter { it.name.contains(searchQuery, true) }
                
                if (tabIndex == 0) {
                    // On Bus Empty State
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(getLocalizedString("Not on a bus yet", "තවම බස් රථයක නැත", "இன்னும் பேருந்தில் இல்லை"), color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getLocalizedString("Scan a QR code to see friends on the same bus", "මිතුරන් බැලීමට QR කේතය ස්කෑන් කරන්න", "QR குறியீட்டை ஸ்கேன் செய்யவும்"), color = Color.LightGray, fontSize = 14.sp)
                    }
                } else if (tabIndex == 1 || tabIndex == 2) {
                    val isAll = tabIndex == 2
                    
                    if (filteredOnline.isEmpty() && (!isAll || filteredOther.isEmpty())) {
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
                                getLocalizedString("No friends found", "මිතුරන් හමුවූයේ නැත", "நண்பர்கள் காணப்படவில்லை"),
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (filteredOnline.isNotEmpty()) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(BusLKBlue, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(getLocalizedString("Online", "මාර්ගගත", "ஆன்லைன்"), style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
                                    }
                                }
                                items(filteredOnline, key = { it.name }) { friend ->
                                    FriendCardV2(friend = friend, onChatClick = { onChatClick(friend.name) })
                                }
                            }
                            
                            if (isAll && filteredOther.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(getLocalizedString("On Other Buses", "වෙනත් බස්වල", "மற்ற பேருந்துகளில்"), style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
                                    }
                                }
                                items(filteredOther, key = { it.name }) { friend ->
                                    FriendCardV2(friend = friend, onChatClick = { onChatClick(friend.name) })
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
fun FriendCardV2(friend: FriendState, onChatClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp, // Modern card-based soft shadow
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE8DBFA), // Light purple from UI
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(friend.initials, color = FriendsPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Active Dot Match
                if (friend.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(
                                if (friend.isOnBus) OpenGreen else BusLKBlue,
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (friend.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = UnreadRed
                        ) {
                            Text(
                                text = friend.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (friend.isOnBus) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    } else {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(friend.statusText, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Chat Button
            Button(
                onClick = onChatClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FriendsPurple
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}