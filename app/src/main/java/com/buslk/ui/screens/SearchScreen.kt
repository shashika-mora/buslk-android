package com.buslk.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buslk.ui.search.BusDetailsUiState
import com.buslk.ui.search.BusDetailsViewModel
import com.buslk.ui.search.RouteDetailsUiState
import com.buslk.ui.search.RouteDetailsViewModel
import com.buslk.ui.search.SearchUiState
import com.buslk.ui.search.SearchViewModel
import com.buslk.ui.theme.BusLKBlue
import com.buslk.ui.theme.FriendsPurple

enum class SearchDestination {
    ROUTE_LIST,
    ROUTE_DETAILS,
    BUS_DETAILS
}

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    routeDetailsViewModel: RouteDetailsViewModel,
    busDetailsViewModel: BusDetailsViewModel
) {
    var searchDestination by rememberSaveable { mutableStateOf(SearchDestination.ROUTE_LIST) }

    BackHandler(enabled = searchDestination != SearchDestination.ROUTE_LIST) {
        searchDestination = when (searchDestination) {
            SearchDestination.BUS_DETAILS -> {
                busDetailsViewModel.clear()
                SearchDestination.ROUTE_DETAILS
            }
            SearchDestination.ROUTE_DETAILS -> {
                routeDetailsViewModel.clear()
                SearchDestination.ROUTE_LIST
            }
            else -> SearchDestination.ROUTE_LIST
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // State-driven navigation
            when (searchDestination) {
                SearchDestination.ROUTE_LIST -> {
                    RouteSelectionView(
                        searchViewModel = searchViewModel,
                        onRouteSelected = { routeId ->
                            routeDetailsViewModel.observeRoute(routeId)
                            searchDestination = SearchDestination.ROUTE_DETAILS
                        }
                    )
                }
                SearchDestination.ROUTE_DETAILS -> {
                    RouteDetailsView(
                        viewModel = routeDetailsViewModel,
                        onBackClicked = {
                            routeDetailsViewModel.clear()
                            searchDestination = SearchDestination.ROUTE_LIST
                        },
                        onBusSelected = { busId ->
                            busDetailsViewModel.loadBus(busId)
                            searchDestination = SearchDestination.BUS_DETAILS
                        }
                    )
                }
                SearchDestination.BUS_DETAILS -> {
                    BusDetailsView(
                        viewModel = busDetailsViewModel,
                        onBackClicked = {
                            busDetailsViewModel.clear()
                            searchDestination = SearchDestination.ROUTE_DETAILS
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSelectionView(
    searchViewModel: SearchViewModel,
    onRouteSelected: (String) -> Unit
) {
    val uiState by searchViewModel.uiState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header & Search Bar
        Surface(color = FriendsPurple, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Route", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        searchViewModel.performSearch(it)
                    },
                    placeholder = { Text("Search by Route No or Name", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = FriendsPurple
                    ),
                    singleLine = true
                )
            }
        }

        // List
        when (val state = uiState) {
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SearchUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { searchViewModel.refreshRoutes() }) {
                        Text("Retry")
                    }
                }
            }
            is SearchUiState.Success -> {
                if (state.routes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No routes found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.routes) { route ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onRouteSelected(route.routeId) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Route ${route.routeId}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${route.startLocation} ⇄ ${route.endLocation}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsView(
    viewModel: RouteDetailsViewModel,
    onBackClicked: () -> Unit,
    onBusSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Active Buses on Route", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FriendsPurple,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        when (val state = uiState) {
            is RouteDetailsUiState.Idle -> {}
            is RouteDetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RouteDetailsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is RouteDetailsUiState.Success -> {
                if (state.buses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No buses registered on this route.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.buses) { item ->
                            val bus = item.busDoc
                            val loc = item.liveLocation
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBusSelected(bus.registrationNumber) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (loc != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.DirectionsBus, 
                                                contentDescription = null, 
                                                tint = if (loc != null) Color(0xFF4CAF50) else Color.Gray, 
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                bus.registrationNumber, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 14.sp, 
                                                color = Color.Black
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (loc != null) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(
                                                    if (loc != null) "LIVE" else "OFFLINE",
                                                    color = if (loc != null) Color(0xFF2E7D32) else Color(0xFF546E7A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Text(
                                            text = "Type: ${bus.type} • Capacity: ${bus.capacity} seats",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Operator: ${bus.owner}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        
                                        if (loc != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Speed: ${loc.speed} km/h • Crowd: ${loc.crowdLevel}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2E7D32)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusDetailsView(
    viewModel: BusDetailsViewModel,
    onBackClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Bus Details", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FriendsPurple,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        when (val state = uiState) {
            is BusDetailsUiState.Idle, is BusDetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is BusDetailsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is BusDetailsUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Static Info Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.busDoc.registrationNumber, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Type: ${state.busDoc.type}", style = MaterialTheme.typography.bodyMedium)
                            Text("Capacity: ${state.busDoc.capacity} Seats", style = MaterialTheme.typography.bodyMedium)
                            Text("Owner: ${state.busDoc.owner}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Telemetry Card
                    if (state.liveLocation != null) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Live Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Current Speed: ${state.liveLocation.speed} km/h", style = MaterialTheme.typography.bodyMedium)
                                Text("Crowd Level: ${state.liveLocation.crowdLevel}", style = MaterialTheme.typography.bodyMedium)
                                Text("Passengers: ${state.liveLocation.activePassengerCount}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Ratings Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ratings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.averageRating > 0) String.format("%.1f", state.averageRating) else "No Ratings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(" (${state.feedbacks.size})", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (state.feedbacks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Recent Feedback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BusLKBlue)
                                state.feedbacks.take(3).forEach { feedback ->
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                    Text(
                                        text = "\"${feedback.comment.ifEmpty { "No comment provided." }}\"", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        feedback.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
