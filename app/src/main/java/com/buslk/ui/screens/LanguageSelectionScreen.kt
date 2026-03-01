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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.buslk.ui.theme.BusLKTheme
import com.buslk.R

/**
 * A screen allowing the user to select their preferred language.
 * 
 * OOD Principle: State Hoisting (Abstraction).
 * This component is visually "stateless". It doesn't decide what happens when a language
 * is clicked; instead, it delegates that decision back up to its parent (MainActivity) 
 * via the `onLanguageSelected` and `onBackClick` lambda functions.
 * 
 * @param onBackClick Callback executed when the top-left back arrow is pressed.
 * @param onLanguageSelected Callback executed when a language button is pressed, passing the language code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBackClick: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    // A customized brand color for this screen's background
    val blueBackground = Color(0xFF1E5DE6)

    // Scaffold provides the standard Material Design screen structure composed of top bars, 
    // bottom bars, and central content area.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
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
        // Column stacks UI elements vertically.
        // We pass the paddingValues from the Scaffold to ensure our content isn't 
        // drawn behind the TopAppBar or system navigation bar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(blueBackground)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacers act like invisible blocks pushing other elements apart mathematically
            Spacer(modifier = Modifier.height(32.dp))

            // Text components render strings with customizable typography
            Text(
                text = stringResource(id = R.string.select_language),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.language_prompt),
                // copy(alpha) is a quick way to create a semi-transparent version of a color
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // We encapsulate the repetitive button UI into a separate Composable function
            // (LanguageButton) to adhere to the DRY (Don't Repeat Yourself) principle.

            // English Button
            LanguageButton(
                text = stringResource(id = R.string.lang_english),
                onClick = { 
                    // Change the app's locale dynamically using Android's AppCompat library
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    // Notify the parent component
                    onLanguageSelected("en") 
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sinhala Button
            LanguageButton(
                text = stringResource(id = R.string.lang_sinhala),
                onClick = { 
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("si"))
                    onLanguageSelected("si") 
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tamil Button
            LanguageButton(
                text = stringResource(id = R.string.lang_tamil),
                onClick = { 
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ta"))
                    onLanguageSelected("ta") 
                }
            )
            
            // A spacer with weight(1f) tells Compose to "take up all remaining vertical space".
            // Since it's at the bottom, it pushes all the buttons up towards the text.
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * A reusable, styled button specific to the language selection screen.
 * 
 * OOD Principle: Reusability and Encapsulation. 
 * By separating this button's design from the main screen layout, we can easily change
 * the style of *all* language buttons by modifying just this one function.
 * 
 * @param text The string physical text displayed inside the button.
 * @param onClick A lambda function capturing the action to perform when clicked.
 */
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

/**
 * A preview function that allows Android Studio to render this screen in the Design editor
 * without needing to run the full application on an emulator.
 */
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

