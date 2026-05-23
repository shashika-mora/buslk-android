package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search as SearchOutlined // Used for "Lost" icon placeholder assuming magnifying glass
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
import com.buslk.data.LostAndFoundDoc
import com.buslk.ui.theme.*
import com.buslk.ui.viewmodels.LostAndFoundViewModel
import com.buslk.ui.viewmodels.LostFoundItemUiStore
import com.buslk.ui.viewmodels.LostAndFoundUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    viewModel: LostAndFoundViewModel,
    onContactClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } 
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf("All Routes") }
    
    // Bottom Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Safely extract the data list from the state, or an empty list if loading/error.
    val actualItems = when (val state = uiState) {
        is LostAndFoundUiState.Success -> state.items
        else -> emptyList()
    }

    // 1. Filter by Search Query
    val searchedItems = if (searchQuery.isBlank()) actualItems else actualItems.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    // 2. Filter by Route Selection
    val routedItems = if (selectedRoute == "All Routes") searchedItems else searchedItems.filter {
        it.route.contains(selectedRoute, ignoreCase = true)
    }

    // 3. Filter by Segment Tab (All / Found / Lost)
    val displayedItems = when (selectedTabIndex) {
        1 -> routedItems.filter { it.isFound }
        2 -> routedItems.filter { !it.isFound }
        else -> routedItems
    }

    // Update the tabs text dynamically with accurate counts
    val allCount = routedItems.size
    val foundCount = routedItems.count { it.isFound }
    val lostCount = routedItems.count { !it.isFound }
    val tabs = listOf("All ($allCount)", "Found ($foundCount)", "Lost ($lostCount)")

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. Purple Header ---
            Surface(
                color = FriendsPurple, // Using the same purple as Friends
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Placeholder icon looking somewhat like a box
                        Icon(
                            imageVector = Icons.Outlined.Search, // Can use a box if we find one, sticking to standard outlined
                            contentDescription = "Lost and Found Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lost & Found",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text("Search items...", color = Color.White.copy(alpha = 0.7f)) 
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

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Route Filters (Scrollable Row)
                    val routes = listOf("All Routes", "Route 138", "Route 176", "Route 120", "Route 177")
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        routes.forEach { route ->
                            val isSelected = route == selectedRoute
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color.White else RoutePillActive,
                                modifier = Modifier
                                    .clickable { selectedRoute = route }
                            ) {
                                Text(
                                    text = route,
                                    color = if (isSelected) FriendsPurple else Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. Post New Item Button ---
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent 
            ) {
                Button(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen), 
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Post New Item", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- 3. Custom Tabs ---
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFFF5F6FA), // Very light gray
                contentColor = Color.Black,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color.Transparent // Hide the default bottom line
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
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

            // --- 4. Content List ---
            when (uiState) {
                is LostAndFoundUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FriendsPurple)
                    }
                }
                is LostAndFoundUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState as LostAndFoundUiState.Error).message, 
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is LostAndFoundUiState.Success -> {
                    if (displayedItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No items found matching criteria.", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(displayedItems, key = { it.id }) { item ->
                                LostFoundCard(item, onContactClick = onContactClick)
                            }
                            
                            // Add some empty space at bottom so FAB doesn't cover last item if we had one
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
        
        // --- 5. Bottom Sheet Form ---
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                NewItemForm(
                    onDismiss = { showBottomSheet = false },
                    onSubmit = { title: String, desc: String, type: String, route: String, loc: String ->
                        viewModel.submitNewItem(
                            title = title,
                            description = desc,
                            itemType = type,
                            routeId = route,
                            location = loc
                        )
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@Composable
fun NewItemForm(onDismiss: () -> Unit, onSubmit: (String, String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("FOUND") }
    var route by remember { mutableStateOf("138") }
    var location by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Report an Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Type Segment
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { type = "LOST" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (type == "LOST") LostOrange.copy(alpha = 0.1f) else Color.Transparent,
                    contentColor = if (type == "LOST") LostOrange else Color.Gray
                ),
                border = BorderStroke(1.dp, if (type == "LOST") LostOrange else Color.LightGray)
            ) { Text("I Lost This") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { type = "FOUND" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (type == "FOUND") FoundBlue.copy(alpha = 0.1f) else Color.Transparent,
                    contentColor = if (type == "FOUND") FoundBlue else Color.Gray
                ),
                border = BorderStroke(1.dp, if (type == "FOUND") FoundBlue else Color.LightGray)
            ) { Text("I Found This") }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What is it? (e.g., Black Umbrella)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description & Appearance") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = route,
                onValueChange = { route = it },
                label = { Text("Route") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location context") },
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSubmit(title, description, type, route, location) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FriendsPurple),
            enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank()
        ) {
            Text("Submit Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LostFoundCard(
    item: LostFoundItemUiStore,
    onContactClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.isFound) FoundBlue else LostOrange
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Simple unicode emoji as icon stand-in for package/magnifying glass
                            Text(if (item.isFound) "📦" else "🔍", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (item.isFound) "Found" else "Lost",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Route Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        color = Color.White
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

                // Status Badge (Open / Claimed)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isClosed) ClaimedYellow else OpenGreen
                ) {
                    Text(
                        text = if (item.isClosed) "Claimed" else "Open",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Body
            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row (Location & Time)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8F9FE) // Very light gray/blue tint
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(item.location, color = Color.Gray, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕒 ", fontSize = 12.sp) // Simple clock
                        Text(item.timeAgo, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported by ${item.reporterName}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                // Outlined Button
                if (!item.isClosed) {
                    OutlinedButton(
                        onClick = { onContactClick(item.reporterName) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (item.isFound) "Contact" else "I Found This!",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
