package com.vincentwang.copilot.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Android counterpart of HomeView.swift.
@Composable
fun HomeScreen(model: HomeModel = viewModel()) {
    val status by model.status.collectAsState()
    val items by model.items.collectAsState()

    LaunchedEffect(Unit) {
        if (status is HomeModel.Status.Idle) model.fetchCloudID()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Your Google User ID",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        StatusContent(status)

        OutlinedButton(onClick = { model.fetchCloudID() }) {
            Text("Refresh")
        }

        if (status is HomeModel.Status.Success) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { model.insertTestData() }) {
                    Text("Insert Sample Items")
                }
                OutlinedButton(onClick = { model.deleteAllItems() }) {
                    Text("Clear All")
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(items, key = { it.id }) { item ->
                    Text(
                        item.aircraftRegistration ?: "—",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatusContent(status: HomeModel.Status) {
    when (status) {
        is HomeModel.Status.Idle, HomeModel.Status.Loading ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Loading…", modifier = Modifier.padding(top = 8.dp))
            }

        is HomeModel.Status.Success ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(status.id, fontFamily = FontFamily.Monospace)
                Text(
                    "Signed in with Google",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        is HomeModel.Status.NotAvailable ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Unavailable")
                Text(
                    "Please sign in with your Google account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

        is HomeModel.Status.Failure ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Error")
                Text(
                    status.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
    }
}
