package com.vincentwang.copilot.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vincentwang.copilot.models.AircraftCategory
import com.vincentwang.copilot.models.AircraftClass

/**
 * FAA category picker with a dependent class picker (14 CFR 61.5), ported
 * from CategoryClassPickers in FlightEntryFormView.swift. The class list
 * follows the chosen category and disappears entirely for categories
 * without class ratings (Powered Lift, Glider).
 */
@Composable
fun CategoryClassPickers(
    category: AircraftCategory?,
    aircraftClass: AircraftClass?,
    onCategoryChange: (AircraftCategory?) -> Unit,
    onClassChange: (AircraftClass?) -> Unit
) {
    PickerRow(
        label = "Category",
        selection = category?.displayName,
        options = AircraftCategory.entries.map { it.displayName }
    ) { selected ->
        val newCategory = AircraftCategory.fromDisplayName(selected)
        onCategoryChange(newCategory)
        // Drop a class that doesn't belong to the new category.
        if (aircraftClass != null && newCategory?.classes?.contains(aircraftClass) != true) {
            onClassChange(null)
        }
    }

    if (category != null && category.classes.isNotEmpty()) {
        PickerRow(
            label = "Class",
            selection = aircraftClass?.displayName,
            options = category.classes.map { it.displayName }
        ) { selected ->
            onClassChange(AircraftClass.fromDisplayName(selected))
        }
    }
}

/** Dropdown row with a bold "Not Specified" default option, standing in
 *  for the iOS navigationLink-style picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerRow(
    label: String,
    selection: String?,
    options: List<String>,
    notSpecifiedLabel: String = "Not Specified",
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selection ?: notSpecifiedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = transparentFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        notSpecifiedLabel,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
