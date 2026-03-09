package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.DarkButtonBg
import com.buslk.ui.theme.FriendsPurple
import com.buslk.ui.theme.UnreadRed
import com.buslk.ui.theme.BusLKBlue

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
    FriendState("Saman De Silva", "SD", "Active 10m ago", true, 0, isOnBus = true) // Faked status for variation
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onChatClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to "Online (2)"
    val tabs = listOf("On Bus", "Online (2)", "All (6)")

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add Friend */ },
                shape = CircleShape,
                containerColor = Color(0xFF00C853), // Green from UI
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Friend")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            // --- 1. Purple Header Sequence ---
            Surface(
                color = FriendsPurple,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // We use a local compose icon as placeholder for the group icon
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Friends Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { 
                            Text("Search friends...", color = Color.White.copy(alpha = 0.7f)) 
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
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

            // --- 2. Custom Tabs ---
            // We use ScrollableTabRow or standard TabRow with custom coloring
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFFF5F6FA), // Very light gray from UI
                contentColor = Color.Black,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color.Transparent // Hide the default bottom line
                    )
                },
                modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(24.dp))
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
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // --- 3. Content List ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Determine which list to show based on tab
                if (selectedTabIndex == 1 || selectedTabIndex == 2) { // 1 = Online, 2 = All
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(BusLKBlue, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Online", style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
                        }
                    }

                    items(mockOnlineFriends) { friend ->
                        FriendCard(friend = friend, onChatClick = { onChatClick(friend.name) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("On Other Buses", style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
                        }
                    }

                    items(mockOtherBusesFriends) { friend ->
                        FriendCard(friend = friend, onChatClick = { onChatClick(friend.name) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (selectedTabIndex == 0) { // On Bus
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Not on a bus yet", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scan a QR code to see friends on the same bus", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendCard(friend: FriendState, onChatClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                // Gradient Circle using solid for simplicity currently
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6B4EE6), // Slightly lighter violet
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(friend.initials, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Active Dot
                if (friend.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(
                                if (friend.isOnBus) Color(0xFF00C853) else BusLKBlue, // Green if on bus, Blue if online
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    if (friend.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
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
                        // Clock icon
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(friend.statusText, color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Chat Button
            Button(
                onClick = onChatClick,
                colors = ButtonDefaults.buttonColors(containerColor = DarkButtonBg),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat", fontSize = 14.sp)
            }
        }
    }
}
