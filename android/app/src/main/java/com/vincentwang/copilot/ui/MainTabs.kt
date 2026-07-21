package com.vincentwang.copilot.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vincentwang.copilot.features.calendar.CalendarScreen
import com.vincentwang.copilot.features.home.HomeScreen
import com.vincentwang.copilot.features.logbook.LogbookScreen
import com.vincentwang.copilot.features.profile.ProfileScreen
import com.vincentwang.copilot.features.report.ReportScreen

// Android counterpart of ContentView.swift's TabView.
private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Outlined.Home),
    Tab("Logbook", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("Calendar", Icons.Outlined.CalendarMonth),
    Tab("Report", Icons.AutoMirrored.Outlined.Assignment),
    Tab("Profile", Icons.Outlined.Person)
)

@Composable
fun MainTabs() {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        androidx.compose.foundation.layout.Box(modifier = content) {
            when (selected) {
                0 -> HomeScreen()
                1 -> LogbookScreen()
                2 -> CalendarScreen()
                3 -> ReportScreen()
                4 -> ProfileScreen()
            }
        }
    }
}
