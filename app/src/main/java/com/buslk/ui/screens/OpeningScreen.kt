package com.buslk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.BusLKTheme

/**
 * The initial intro screen displayed when the app first launches for a new user.
 * 
 * OOD Principle: State Hoisting (Abstraction).
 * This component is completely stateless. It displays static UI and simply calls 
 * the `onGetStartedClick` lambda when the user presses the main button, letting 
 * the parent component (MainActivity) handle the actual navigation logic.
 * 
 * @param onGetStartedClick Callback executed when the "Get Started" button is clicked.
 */
@Composable
fun OpeningScreen(
    onGetStartedClick: () -> Unit
) {
    // Defines the primary brand background color
    val blueBackground = Color(0xFF1E5DE6)
    
    // Column stacks all child UI elements vertically from top to bottom
    Column(
        modifier = Modifier
            .fillMaxSize()           // Takes up the entire screen space available
            .background(blueBackground) // Applies the background color
            .padding(24.dp),         // Adds a 24 density-independent pixel gap around the edges
        horizontalAlignment = Alignment.CenterHorizontally // Centers all children horizontally
    ) {
        // A Spacer with weight(1f) tells Compose to "take up all available empty space".
        // Placed at the top, it pushes everything else downwards.
        Spacer(modifier = Modifier.weight(1f))
        
        // --- Logo Section ---
        // Surface is a Material Design component that provides background, elevation, and shape.
        // We use it here to draw a white circle behind the bus icon.
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(100.dp) // Sets both width and height to 100dp
        ) {
            // Box allows us to place elements inside it and align them precisely.
            // Using Alignment.Center perfectly centers the icon inside the circle.
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus, // The built-in Material bus icon
                    contentDescription = "BusLK Logo",        // Crucial for accessibility (screen readers)
                    tint = blueBackground,                    // The icon color (matches the screen background)
                    modifier = Modifier.size(50.dp)           // Size of the actual icon graphic
                )
            }
        }
        
        // Fixed-size spacer providing empty vertical space
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Title Section ---
        Text(
            text = stringResource(id = com.buslk.R.string.app_name), // Loads string from res/values/strings.xml
            color = Color.White,
            fontSize = 40.sp,                                        // Scalable Pixels (respects user's font size settings)
            fontWeight = FontWeight.Bold                             // Makes the text thick
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // --- Subtitle Section ---
        Text(
            text = stringResource(id = com.buslk.R.string.opening_subtitle),
            color = Color.White,
            fontSize = 16.sp
        )
        
        // Another Spacer with weight(1f). 
        // Because there's a weight(1f) at the top and a weight(1f) below the subtitle,
        // the content between them (logo + title + subtitle) is perfectly vertically centered!
        Spacer(modifier = Modifier.weight(1f))
        
        // --- Main Action Section ---
        Button(
            onClick = onGetStartedClick,
            modifier = Modifier
                .fillMaxWidth()     // Makes the button stretch horizontally to the screen edges
                .height(56.dp),     // Standard Touch Target height for Material Design
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp) // Gives the button gently curved corners
        ) {
            Text(
                text = stringResource(id = com.buslk.R.string.btn_get_started),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // --- Footer Section ---
        Text(
            text = stringResource(id = com.buslk.R.string.opening_footer),
            // copy(alpha) gives us a semi-transparent version of white, making it look greyed out
            color = Color.White.copy(alpha = 0.8f), 
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * A preview function that allows Android Studio to render this screen in the Design editor
 * without needing to run the full application on an emulator.
 */
@Preview(showBackground = true)
@Composable
fun OpeningScreenPreview() {
    BusLKTheme {
        OpeningScreen(onGetStartedClick = {})
    }
}

