package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.BusLKTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBackClick: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val blueBackground = Color(0xFF1E5DE6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = blueBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(blueBackground)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Select Language",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "භාෂාව තෝරන්න / மொழியைத் தேர்ந்தெடுக்கவும்",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // English Button
            LanguageButton(
                text = "English",
                onClick = { onLanguageSelected("en") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sinhala Button
            LanguageButton(
                text = "සිංහල",
                onClick = { onLanguageSelected("si") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tamil Button
            LanguageButton(
                text = "தமிழ்",
                onClick = { onLanguageSelected("ta") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LanguageButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1E5DE6) // Blue text on white button
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionScreenPreview() {
    BusLKTheme {
        LanguageSelectionScreen(
            onBackClick = {},
            onLanguageSelected = {}
        )
    }
}
