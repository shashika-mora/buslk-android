package com.buslk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.buslk.R
import com.buslk.data.BusDoc
import com.buslk.data.RouteDoc
import com.buslk.ui.search.SearchUiState

// --- New Imports for Full Search Screen ---
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.Color
import com.buslk.ui.theme.BusLKBlue
import com.buslk.ui.theme.QuickActionBg
import com.buslk.ui.theme.TrendingOrange



@Composable
fun RouteResultCard(route: RouteDoc, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Route ${route.routeId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${route.distanceKm} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${route.startLocation} ⇄ ${route.endLocation}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (route.stops.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${route.stops.size} ${stringResource(R.string.search_stops)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BusResultCard(bus: BusDoc, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bus.registrationNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.search_runs_on)}: ${bus.defaultRouteId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ==========================================
// FULL SEARCH SCREEN UI (For the Bottom Nav Tab)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Blue Header Section ---
        Surface(
            color = BusLKBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar Input Field
                val textState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                OutlinedTextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    placeholder = {
                        Text("Search routes, stops, locations...", color = Color.Gray)
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = BusLKBlue
                    ),
                    singleLine = true
                )
            }
        }

        // --- Content Scrollable Area ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- 2. Recent Searches ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val recents = listOf("Mount Lavinia", "Route 138", "Pettah Bus Stand", "Dehiwala Junction")
            recents.forEach { query ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(query, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. Trending Searches ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = TrendingOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trending Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Hardcoded "Flow" layout using Rows
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendingPill("Colombo Fort")
                TrendingPill("Route 177")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendingPill("Galle Road")
                TrendingPill("Bambalapitiya")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. Quick Actions ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = QuickActionBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nearby Stops Action
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BusLKBlue)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nearby Stops", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Live Traffic Action
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Color(0xFF00C853)) // Green
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Live Traffic", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        color = Color.White
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
