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
import com.buslk.data.BusDoc
import com.buslk.data.RouteDoc
import com.buslk.ui.search.SearchUiState

/**
 * Composable that displays the content inside the expanded SearchBar.
 * It observes the immutable [SearchUiState] and draws the appropriate UI.
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
                    text = "Type a route number (e.g. 138) or bus registration",
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