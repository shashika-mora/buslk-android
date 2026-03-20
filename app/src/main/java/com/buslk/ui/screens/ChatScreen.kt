package com.buslk.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.buslk.ui.theme.BusLKBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendName: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(friendName, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BusLKBlue
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                var message by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = { message = "" }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = BusLKBlue)
                }
            }
        }
    ) { paddingValues ->

    }
}