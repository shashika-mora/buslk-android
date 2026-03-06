package com.buslk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buslk.R
import com.buslk.ui.auth.AuthViewModel

/**
 * A temporary profile screen used to test the Logout flow.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    authViewModel.signOut()
                    onLogoutSuccess()
                }
            ) {
                Text(text = stringResource(R.string.action_logout))
            }
        }
    }
}
