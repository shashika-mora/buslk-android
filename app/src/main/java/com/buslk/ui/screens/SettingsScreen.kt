package com.buslk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.R
import com.buslk.ui.auth.AuthViewModel
import com.buslk.ui.viewmodels.SettingsViewModel
import com.buslk.ui.theme.BusLKBlue
import com.buslk.ui.theme.UnreadRed
import com.buslk.ui.theme.LightGrayBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onLogoutSuccess: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Often better for Dark Mode
                )
            )
        },
        containerColor = LightGrayBg // Replaced hardcoded grey 0xFFF5F6FA
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Account Control ---
            item {
                SettingsSection(title = stringResource(R.string.settings_account_control)) {
                    SettingsRow(icon = Icons.Outlined.Person, title = stringResource(R.string.settings_edit_profile), onClick = {})
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    SettingsRow(icon = Icons.Outlined.Lock, title = stringResource(R.string.settings_change_password), onClick = {})
                }
            }

            // --- UI Control ---
            item {
                SettingsSection(title = stringResource(R.string.settings_ui_control)) {
                    val currentThemeMode by settingsViewModel.themeMode.collectAsState()
                    var themeDropdownExpanded by remember { mutableStateOf(false) }

                    val themeText = when (currentThemeMode) {
                        1 -> stringResource(R.string.settings_theme_light)
                        2 -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    }

                    // Theme Dropdown Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = BusLKBlue)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.settings_app_theme), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(themeText, color = Color.Gray, fontSize = 14.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                            }

                            DropdownMenu(
                                expanded = themeDropdownExpanded,
                                onDismissRequest = { themeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.settings_theme_system)) }, onClick = { settingsViewModel.updateThemeMode(0); themeDropdownExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.settings_theme_light)) }, onClick = { settingsViewModel.updateThemeMode(1); themeDropdownExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.settings_theme_dark)) }, onClick = { settingsViewModel.updateThemeMode(2); themeDropdownExpanded = false })
                            }
                        }
                    }
                }
            }

            // --- Preferences ---
            item {
                SettingsSection(title = stringResource(R.string.settings_user_preferences)) {
                    // Default Route is non-functional MVP fallback for now, maybe wired later.
                    SettingsRow(icon = Icons.Outlined.DirectionsBus, title = stringResource(R.string.settings_default_route), subtitle = stringResource(R.string.settings_route_138), onClick = {})
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    
                    val currentLanguage by settingsViewModel.appLanguage.collectAsState()
                    var langDropdownExpanded by remember { mutableStateOf(false) }
                    
                    val langText = when (currentLanguage) {
                        "si" -> stringResource(R.string.lang_sinhala)
                        "ta" -> stringResource(R.string.lang_tamil)
                        else -> stringResource(R.string.lang_english)
                    }

                    // Language Dropdown Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { langDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = BusLKBlue)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.settings_language), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(langText, color = Color.Gray, fontSize = 14.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                            }

                            DropdownMenu(
                                expanded = langDropdownExpanded,
                                onDismissRequest = { langDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.lang_english)) }, onClick = { settingsViewModel.updateLanguage("en"); langDropdownExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.lang_sinhala)) }, onClick = { settingsViewModel.updateLanguage("si"); langDropdownExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.lang_tamil)) }, onClick = { settingsViewModel.updateLanguage("ta"); langDropdownExpanded = false })
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    
                    val pushNotifications by settingsViewModel.notificationsEnabled.collectAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = BusLKBlue)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.settings_push_notifications), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = pushNotifications,
                            onCheckedChange = { settingsViewModel.updateNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = BusLKBlue)
                        )
                    }
                }
            }
            
            // --- Logout Button ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        authViewModel.signOut()
                        onLogoutSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnreadRed), // Replaced hardcoded Red 0xFFFF3B30
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_logout), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            // MaterialTheme handles dark/light automatically via BusLKTheme
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BusLKBlue)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}
