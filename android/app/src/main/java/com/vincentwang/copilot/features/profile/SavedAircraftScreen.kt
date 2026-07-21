package com.vincentwang.copilot.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.data.AircraftProfile
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.features.report.SubScreenTopBar
import com.vincentwang.copilot.models.AircraftCategory
import com.vincentwang.copilot.models.AircraftClass
import com.vincentwang.copilot.ui.components.CategoryClassPickers
import com.vincentwang.copilot.ui.components.EmptyState
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.FormTextField
import com.vincentwang.copilot.ui.components.FullScreenSheet
import java.util.UUID

/**
 * Management screen for saved aircraft profiles, ported from
 * SavedAircraftView.swift. A profile stores the registration and type
 * once so the flight form can fill them in with one tap. Profiles are
 * entirely optional — the form always accepts free-typed aircraft too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAircraftScreen(onBack: () -> Unit, model: LogbookViewModel = viewModel()) {
    val aircraft by model.aircraft.collectAsState()

    var isAdding by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Aircraft") },
                navigationIcon = { SubScreenBackButton(onBack) },
                actions = {
                    IconButton(onClick = { isAdding = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Aircraft")
                    }
                }
            )
        }
    ) { padding ->
        if (aircraft.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.FlightTakeoff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No Saved Aircraft",
                description = "Save an aircraft you fly often and the flight form " +
                    "can fill it in for you.",
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                FormSection {
                    aircraft.forEachIndexed { index, plane ->
                        if (index > 0) HorizontalDivider()
                        // Display name on top; registration, type, class below.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingId = plane.id }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                plane.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            val details = listOfNotNull(
                                plane.registration, plane.aircraftType,
                                plane.aircraftClass ?: plane.category
                            ).filter { it.isNotEmpty() }.joinToString(" · ")
                            if (details.isNotEmpty()) {
                                Text(
                                    details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAdding) {
        AircraftFormSheet(
            aircraft = null,
            onSave = model::saveAircraft,
            onDelete = null,
            onDismiss = { isAdding = false }
        )
    }
    val editing = aircraft.firstOrNull { it.id == editingId }
    if (editing != null) {
        AircraftFormSheet(
            aircraft = editing,
            onSave = model::saveAircraft,
            // Logged flights keep their own copy of the aircraft fields, so
            // deleting a profile never touches the logbook.
            onDelete = {
                model.deleteAircraft(editing)
                editingId = null
            },
            onDismiss = { editingId = null }
        )
    }
}

@Composable
private fun SubScreenBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

/** Sheet for creating or editing one aircraft profile (AircraftFormView
 *  on iOS). Registration is the only required field. */
@Composable
fun AircraftFormSheet(
    aircraft: AircraftProfile?,
    onSave: (AircraftProfile) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(aircraft?.name.orEmpty()) }
    var registration by remember { mutableStateOf(aircraft?.registration.orEmpty()) }
    var aircraftType by remember { mutableStateOf(aircraft?.aircraftType.orEmpty()) }
    var category by remember {
        mutableStateOf(AircraftCategory.fromDisplayName(aircraft?.category))
    }
    var aircraftClass by remember {
        mutableStateOf(AircraftClass.fromDisplayName(aircraft?.aircraftClass))
    }

    val isValid = registration.isNotBlank()

    FullScreenSheet(
        title = if (aircraft == null) "New Aircraft" else "Edit Aircraft",
        onDismissRequest = onDismiss,
        leadingLabel = "Cancel",
        leadingColor = MaterialTheme.colorScheme.error,
        onLeadingClick = onDismiss,
        trailingLabel = "Save",
        trailingEnabled = isValid,
        onTrailingClick = {
            onSave(
                AircraftProfile(
                    id = aircraft?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    registration = registration.trim().uppercase(),
                    aircraftType = aircraftType.trim().uppercase(),
                    category = category?.displayName,
                    aircraftClass = aircraftClass?.displayName
                )
            )
            onDismiss()
        }
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            FormSection {
                FormTextField(
                    placeholder = "Registration (e.g. N12345)",
                    value = registration,
                    onValueChange = { registration = it },
                    capitalize = true
                )
                FormTextField(
                    placeholder = "Aircraft Type (e.g. C172)",
                    value = aircraftType,
                    onValueChange = { aircraftType = it },
                    capitalize = true
                )
                CategoryClassPickers(
                    category = category,
                    aircraftClass = aircraftClass,
                    onCategoryChange = { category = it },
                    onClassChange = { aircraftClass = it }
                )
            }
            FormSection(
                footer = "Optional. When left empty, the profile is shown by its registration."
            ) {
                FormTextField(
                    placeholder = "Name (e.g. Club 172)",
                    value = name,
                    onValueChange = { name = it }
                )
            }
            if (onDelete != null) {
                FormSection {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Aircraft", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
