package com.vincentwang.copilot.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.BuildConfig
import com.vincentwang.copilot.data.AppPrefs
import com.vincentwang.copilot.features.auth.AuthModel
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.features.report.SubScreenTopBar
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.FormTextField

/**
 * Pilot identity, Google account status, and (debug builds only)
 * sample-data utilities. Ported from ProfileView.swift, with the iCloud
 * section mapped to the Google/Firebase account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: AuthModel = viewModel(),
    model: LogbookViewModel = viewModel()
) {
    val authState by auth.state.collectAsState()
    val pilotName by AppPrefs.pilotName.collectAsState()
    val certificateNumber by AppPrefs.certificateNumber.collectAsState()

    var showingAircraft by remember { mutableStateOf(false) }
    var showingSettings by remember { mutableStateOf(false) }

    if (showingAircraft) {
        BackHandler { showingAircraft = false }
        SavedAircraftScreen(onBack = { showingAircraft = false })
        return
    }
    if (showingSettings) {
        BackHandler { showingSettings = false }
        SettingsScreen(onBack = { showingSettings = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { showingSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Name and certificate number (device-local preference data).
            FormSection(title = "Pilot") {
                FormTextField(
                    placeholder = "Name",
                    value = pilotName,
                    onValueChange = AppPrefs::setPilotName
                )
                FormTextField(
                    placeholder = "Certificate Number",
                    value = certificateNumber,
                    onValueChange = AppPrefs::setCertificateNumber
                )
            }

            // Saved aircraft profiles (optional shortcuts for the flight form).
            FormSection(
                title = "Aircraft",
                footer = "Save aircraft you fly often so the flight form can fill in " +
                    "the registration and type for you."
            ) {
                FormRow(modifier = Modifier.clickable { showingAircraft = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.FlightTakeoff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text("My Aircraft", modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Google-account sync status and local sign-out (iCloud on iOS).
            FormSection(
                title = "Google Account",
                footer = "Your logbook is stored in your personal Firebase account " +
                    "and syncs across your devices automatically."
            ) {
                when (val s = authState) {
                    is AuthModel.State.SignedIn -> {
                        AccountRow(
                            icon = { Icon(Icons.Outlined.CloudDone, null,
                                tint = MaterialTheme.colorScheme.primary) },
                            text = "Synced with Google account"
                        )
                        FormRow(modifier = Modifier.clickable { auth.signOutLocally() }) {
                            Text(
                                "Sign Out on This Device",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is AuthModel.State.SignedOut -> AccountRow(
                        icon = { Icon(Icons.Outlined.CloudOff, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "Not signed in with Google"
                    )
                    is AuthModel.State.Idle, AuthModel.State.Checking -> AccountRow(
                        icon = { Icon(Icons.Outlined.Cloud, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "Checking Google account…"
                    )
                    is AuthModel.State.Error -> AccountRow(
                        icon = { Icon(Icons.Outlined.CloudOff, null,
                            tint = MaterialTheme.colorScheme.error) },
                        text = s.message
                    )
                }
            }

            // Sample-data helpers for development builds.
            if (BuildConfig.DEBUG) {
                FormSection(title = "Developer") {
                    FormRow(modifier = Modifier.clickable { model.insertSampleData() }) {
                        Text("Insert Sample Flights", color = MaterialTheme.colorScheme.primary)
                    }
                    FormRow(modifier = Modifier.clickable { model.deleteAllData() }) {
                        Text("Delete All Flights", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(icon: @Composable () -> Unit, text: String) {
    FormRow {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(text, modifier = Modifier.padding(start = 12.dp))
        }
    }
}
