package com.vincentwang.copilot.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vincentwang.copilot.data.AppPrefs
import com.vincentwang.copilot.features.report.SubScreenTopBar
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection

/**
 * App preferences, reached from the gear button on the Profile tab.
 * Ported from SettingsView.swift.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val nvgLoggingEnabled by AppPrefs.nvgLoggingEnabled.collectAsState()

    Scaffold(
        topBar = { SubScreenTopBar(title = "Settings", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FormSection(
                title = "Logbook",
                footer = "Shows night-vision-goggle time in the flight form and its " +
                    "total on the Report tab. Already-logged NVG time is kept either way."
            ) {
                FormRow {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable NVG Logging", modifier = Modifier.weight(1f))
                        Switch(
                            checked = nvgLoggingEnabled,
                            onCheckedChange = AppPrefs::setNvgLoggingEnabled
                        )
                    }
                }
            }
        }
    }
}
