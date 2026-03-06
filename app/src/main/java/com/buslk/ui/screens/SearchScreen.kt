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

/**
 * Composable that displays the content inside the expanded SearchBar.
 * 
 * Architecture Principle: Unidirectional Data Flow (UDF).
 * This component has no internal `MutableState` defining business logic. It simply observers the 
 * purely immutable [SearchUiState] passed down to it ("State flows down"), and when the user 
 * taps a route or bus, it triggers the callback functions passed as arguments ("Events flow up").
 */
@Composable
fun SearchContent(
    uiState: SearchUiState,
    onRouteClick: (RouteDoc) -> Unit,
    onBusClick: (BusDoc) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is SearchUiState.Idle -> {
                Text(
                    text = stringResource(R.string.search_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is SearchUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SearchUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is SearchUiState.Success -> {
                if (uiState.routes.isEmpty() && uiState.buses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.routes.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.search_routes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(items = uiState.routes, key = { it.routeId }) { route ->
                                RouteResultCard(route = route, onClick = { onRouteClick(route) })
                            }
                        }

                        if (uiState.buses.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.search_buses),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(items = uiState.buses, key = { it.registrationNumber }) { bus ->
                                BusResultCard(bus = bus, onClick = { onBusClick(bus) })
                            }
                        }
                    }
                }
            }
        }
    }
}

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
