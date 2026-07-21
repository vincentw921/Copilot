package com.vincentwang.copilot.features.logbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.vincentwang.copilot.data.Item
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.ui.components.EmptyState
import com.vincentwang.copilot.ui.components.FlightRow
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * The main logbook: every flight, newest first, grouped by month.
 * Tap a row to view/edit, swipe to delete, "+" to log a new flight.
 * Ported from LogbookView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(model: LogbookViewModel = viewModel()) {
    val items by model.items.collectAsState()
    val savedAircraft by model.aircraft.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var isAddingFlight by remember { mutableStateOf(false) }
    // Track the id (not the value) so the open sheet refreshes after edits.
    var viewingItemId by remember { mutableStateOf<String?>(null) }

    // Entries matching the search text (all entries when the search is empty).
    val filteredItems = remember(items, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) items
        else items.filter { item ->
            listOf(
                item.aircraftRegistration, item.aircraftType,
                item.departureAirport, item.arrivalAirport
            ).any { it?.contains(query, ignoreCase = true) == true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logbook") },
                actions = {
                    IconButton(onClick = { isAddingFlight = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Flight")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Filled.Flight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = "No Flights Logged",
                    description = "Tap + to log your first flight."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Registration, type, or airport") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Consecutive month groups ("July 2026") built from the
                    // date-sorted list.
                    monthSections(filteredItems).forEach { (title, sectionItems) ->
                        item(key = "header-$title") {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 16.dp, top = 16.dp, bottom = 4.dp
                                )
                            )
                        }
                        sectionItems.forEach { item ->
                            item(key = item.id) {
                                DismissableFlightRow(
                                    item = item,
                                    onClick = { viewingItemId = item.id },
                                    onDelete = { model.deleteItem(item) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingFlight) {
        FlightEntryFormSheet(
            existing = null,
            savedAircraft = savedAircraft,
            onSave = model::saveEntry,
            onDismiss = { isAddingFlight = false }
        )
    }

    // Look the entry up fresh each recomposition so the read-only preview
    // shows edits as soon as they save (like @ObservedObject on iOS).
    val viewingItem = items.firstOrNull { it.id == viewingItemId }
    if (viewingItem != null) {
        FlightDetailSheet(
            entry = FlightEntry.fromItem(viewingItem),
            savedAircraft = savedAircraft,
            onSave = model::saveEntry,
            onDismiss = { viewingItemId = null }
        )
    }
}

/** Swipe-to-delete wrapper around a tappable flight row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissableFlightRow(
    item: Item,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        // Opaque so the delete affordance only shows mid-swipe.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            FlightRow(FlightEntry.fromItem(item))
        }
    }
}

/** Consecutive month groups from the date-sorted list (monthSections on iOS). */
private fun monthSections(items: List<Item>): List<Pair<String, List<Item>>> {
    val sections = mutableListOf<Pair<String, MutableList<Item>>>()
    for (item in items) {
        val entry = FlightEntry.fromItem(item)
        val title = entry.date.format(monthFormat)
        if (sections.lastOrNull()?.first == title) {
            sections.last().second.add(item)
        } else {
            sections.add(title to mutableListOf(item))
        }
    }
    return sections
}
