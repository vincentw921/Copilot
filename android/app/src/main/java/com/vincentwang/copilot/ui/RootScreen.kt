package com.vincentwang.copilot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.features.auth.AuthModel
import com.vincentwang.copilot.features.auth.AuthScreen

// Android counterpart of RootView in CopilotApp.swift.
@Composable
fun RootScreen(auth: AuthModel = viewModel()) {
    val state by auth.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state is AuthModel.State.Idle) auth.start() // auto-login on launch
    }

    when (state) {
        is AuthModel.State.Idle, AuthModel.State.Checking ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Checking Google account…")
            }

        is AuthModel.State.SignedOut, is AuthModel.State.Error ->
            AuthScreen(model = auth)      // dedicated authentication screen

        is AuthModel.State.SignedIn ->
            MainTabs()                    // the real app UI
    }
}
