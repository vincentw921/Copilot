package com.vincentwang.copilot.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// Android counterpart of AuthView.swift.
@Composable
fun AuthScreen(model: AuthModel = viewModel()) {
    val state by model.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (state is AuthModel.State.Idle) model.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            "Google Sign-In",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        when (val s = state) {
            is AuthModel.State.Idle, AuthModel.State.Checking ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Checking Google account…", modifier = Modifier.padding(top = 8.dp))
                }

            is AuthModel.State.SignedIn ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Signed in with Google",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(s.cloudID, fontFamily = FontFamily.Monospace)
                }

            is AuthModel.State.SignedOut ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Not signed in with Google.")
                    Text(
                        "Sign in with your Google account to sync your logbook.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { model.signIn(context) }) {
                        Text("Sign in with Google")
                    }
                }

            is AuthModel.State.Error ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Error", fontWeight = FontWeight.Bold)
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { model.refresh() } }) {
                Text("Refresh")
            }
            if (state is AuthModel.State.SignedIn) {
                OutlinedButton(onClick = { model.signOutLocally() }) {
                    Text("Sign Out (Local)")
                }
            }
        }
    }
}
