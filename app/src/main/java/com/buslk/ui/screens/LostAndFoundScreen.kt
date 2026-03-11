package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.BusLKTheme
import com.buslk.ui.theme.Purple40
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf("All Routes") }
    var selectedTab by remember { mutableStateOf(0) }
    var showPostDialog by remember { mutableStateOf(false) }

    val routes = listOf("All Routes", "Route 138", "Route 176", "Route 120", "Route 177")
    val tabs = listOf("All (6)", "Found (4)", "Lost (2)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Light gray background
    ) {
        // Top Section with Purple Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8B00FF)) // Specific purple color
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Box",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lost & Found",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1.5f)) // Center alignment trick
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search items...", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        disabledContainerColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Scrollable routes could be in a LazyRow, but for simplicity we wrap them
                    routes.forEach { route ->
                        FilterChipCustom(
                            text = route,
                            isSelected = selectedRoute == route,
                            onClick = { selectedRoute = route }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Post New Item Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = { showPostDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B803B)) // specific green
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post New Item", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFF3F4F6),
            contentColor = Color.Black,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(40.dp),
            indicator = { /* Hide default indicator to match custom design */ },
            divider = { }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    modifier = Modifier
                        .padding(2.dp)
                        .background(
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Item List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mock items
            items(10) { index ->
                LostFoundItemCard(
                    status = if (index % 3 == 0) "Lost" else "Found",
                    route = if (index % 2 == 0) "Route 138" else "Route 176",
                    isOpen = index % 4 != 0,
                    title = if (index % 3 == 0) "Blue Umbrella" else "Black Backpack",
                    description = if (index % 3 == 0) "Left on the bus yesterday morning." else "Small black backpack with laptop inside. Found near seat 12A.",
                    location = if (index % 2 == 0) "Seat 12A" else "Near door",
                    time = "${index * 15} min ago",
                    reportedBy = if (index % 2 == 0) "Amal P." else "Kasun N."
                )
            }
        }
    }

    if (showPostDialog) {
        PostNewItemDialog(
            onDismiss = { showPostDialog = false },
            onPost = { showPostDialog = false }
        )
    }
}

@Composable
fun FilterChipCustom(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF8B00FF) else Color.White, // Match purple or white
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LostFoundItemCard(
    status: String,
    route: String,
    isOpen: Boolean,
    title: String,
    description: String,
    location: String,
    time: String,
    reportedBy: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = if (status == "Found") Color(0xFF1877F2) else Color(0xFFFFA500),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📦 $status",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Route Tag
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = route,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Open/Closed Tag
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isOpen) Color(0xFF1B803B) else Color.Red, RoundedCornerShape(16.dp))
                        .background(if (isOpen) Color(0xFF1B803B) else Color.Red, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOpen) "Open" else "Closed",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Description
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location and Time
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = location, color = Color.Gray, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(text = "🕒 $time", color = Color.Gray, fontSize = 12.sp)
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported by $reportedBy",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = null // Removed border to match "Contact" button style slightly or use filled
                ) {
                    Text("Contact", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostNewItemDialog(
    onDismiss: () -> Unit,
    onPost: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Post New Item") },
        text = {
            Column {
                var message by remember { mutableStateOf("") }
                var isFound by remember { mutableStateOf(true) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isFound, onClick = { isFound = true })
                    Text("Found")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isFound, onClick = { isFound = false })
                    Text("Lost")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Mock Photo upload button
                OutlinedButton(
                    onClick = { /* Handle photo upload */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo")
                }
            }
        },
        confirmButton = {
            Button(onClick = onPost) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LostAndFoundScreenPreview() {
    BusLKTheme {
        LostAndFoundScreen(onBackClick = {})
    }
}
