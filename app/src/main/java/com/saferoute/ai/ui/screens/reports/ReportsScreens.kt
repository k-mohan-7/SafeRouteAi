package com.saferoute.ai.ui.screens.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.saferoute.ai.ui.components.EmptyState
import com.saferoute.ai.ui.components.IncidentCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    onIncidentClick: (Double, Double) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    var tab by remember { mutableIntStateOf(0) }
    val state by viewModel.uiState.collectAsState()
    val filters = listOf("All", "Accident", "Road Block", "Waterlogging", "Tree Fall", "Construction", "Flood", "Traffic Jam", "Other")

    LaunchedEffect(tab) {
        if (tab == 0) viewModel.loadMyReports() else viewModel.loadAllReports()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Reports") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My Reports") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("All Reports") })
            }
            if (tab == 1) {
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter) }
                        )
                    }
                }
            }
            if (state.isLoading) CircularProgressIndicator(Modifier.padding(16.dp))
            val list = if (tab == 0) state.myReports else viewModel.filteredReports()
            if (list.isEmpty() && !state.isLoading) {
                EmptyState("No incidents found")
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { incident ->
                        IncidentCard(
                            incident = incident,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            onClick = { onIncidentClick(incident.latitude, incident.longitude) },
                            trailing = if (tab == 0 && incident.status == "active") {
                                {
                                    Button(onClick = { viewModel.resolveIncident(incident.id) }) {
                                        Text("Resolve")
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
