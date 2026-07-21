package com.vincentwang.copilot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

// Reusable building blocks shared by every screen: the iOS-style grouped
// form sections, labeled value rows, and the inputs used by the flight
// and aircraft forms.

/** Formats decimal hours with one fractional digit ("1.5", "0.0"). */
fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

/** Grouped section: small-caps header, rounded card of rows, footnote. */
@Composable
fun FormSection(
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
    }
}

/** Standard row layout inside a FormSection. */
@Composable
fun FormRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}

/** Label with right-aligned decimal hours (HoursRow on iOS). */
@Composable
fun HoursRow(label: String, hours: Double) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(
                formatHours(hours),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Label with a right-aligned whole-number count (CountRow on iOS). */
@Composable
fun CountRow(label: String, count: Int) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(
                "$count",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Label with a right-aligned free-text value (LabeledContent on iOS). */
@Composable
fun LabeledRow(label: String, value: String) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(2f)
            )
        }
    }
}

/**
 * Labeled decimal-hours input, right-aligned like a paper logbook column
 * (HoursField on iOS). A zero value renders as an empty field (grey "0.0"
 * placeholder) so the pilot can type straight away without clearing a
 * default first.
 */
@Composable
fun HoursField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    // Local text so partial input ("1.") isn't clobbered mid-typing;
    // resynced whenever the value is changed from outside (checkbox fills).
    var text by remember { mutableStateOf(if (value == 0.0) "" else trimHours(value)) }
    LaunchedEffect(value) {
        if ((text.toDoubleOrNull() ?: 0.0) != value) {
            text = if (value == 0.0) "" else trimHours(value)
        }
    }

    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            TextField(
                value = text,
                onValueChange = { new ->
                    if (new.isEmpty() || new.toDoubleOrNull() != null) {
                        text = new
                        onValueChange(new.toDoubleOrNull() ?: 0.0)
                    }
                },
                placeholder = { Text("0.0", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                colors = transparentFieldColors(),
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

/** Renders a Double without a trailing ".0" ("1.5", "2"). */
private fun trimHours(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString()

/** Labeled whole-number input with -/+ steppers (CountField on iOS). */
@Composable
fun CountField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(
                "$value",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                enabled = value > 0
            ) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Decrease $label")
            }
            IconButton(
                onClick = { if (value < 99) onValueChange(value + 1) },
                enabled = value < 99
            ) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Increase $label")
            }
        }
    }
}

/** Collapsible group header + content (DisclosureGroup on iOS). */
@Composable
fun DisclosureGroup(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(content = content)
        }
    }
}

/** Borderless text field used inside form rows. */
@Composable
fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

/** Plain-text field row for forms (TextField in an iOS Form section). */
@Composable
fun FormTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    capitalize: Boolean = false,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = { onValueChange(if (capitalize) it.uppercase() else it) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        colors = transparentFieldColors(),
        keyboardOptions = if (capitalize) {
            KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false)
        } else {
            KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Full-screen modal used wherever iOS presents a sheet: a scaffold with
 * a centered title, an optional leading action (Cancel/Edit, red/blue
 * tinted like the iOS toolbar buttons), and a trailing action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenSheet(
    title: String,
    onDismissRequest: () -> Unit,
    leadingLabel: String? = null,
    leadingColor: Color = MaterialTheme.colorScheme.primary,
    onLeadingClick: (() -> Unit)? = null,
    trailingLabel: String? = null,
    trailingColor: Color = MaterialTheme.colorScheme.primary,
    trailingEnabled: Boolean = true,
    onTrailingClick: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (leadingLabel != null && onLeadingClick != null) {
                                TextButton(onClick = onLeadingClick) {
                                    Text(leadingLabel, color = leadingColor)
                                }
                            }
                        },
                        actions = {
                            if (trailingLabel != null && onTrailingClick != null) {
                                TextButton(onClick = onTrailingClick, enabled = trailingEnabled) {
                                    Text(
                                        trailingLabel,
                                        color = if (trailingEnabled) trailingColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                content(Modifier.padding(padding))
            }
        }
    }
}

/** Centered empty-state message (ContentUnavailableView on iOS). */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
