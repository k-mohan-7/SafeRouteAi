package com.saferoute.ai.ui.screens.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.saferoute.ai.domain.model.PlannedRoute
import com.saferoute.ai.domain.model.RiskCalculator
import com.saferoute.ai.ui.components.RiskBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    sourceLat: Double?,
    sourceLng: Double?,
    sourceLabel: String = "My Location",
    onRoutePlanned: (PlannedRoute, List<PlannedRoute>) -> Unit,
    onBack: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(sourceLat, sourceLng) {
        if (sourceLat != null && sourceLng != null) viewModel.setSource(sourceLat, sourceLng)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("From", style = MaterialTheme.typography.labelMedium)
                            Text(sourceLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.destinationQuery,
                        onValueChange = viewModel::onDestinationQueryChange,
                        label = { Text("Where to?") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.isSearching) CircularProgressIndicator(Modifier.size(20.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchOnEnter() })
                    )
                }
            }

            if (state.suggestions.isNotEmpty() && state.plannedRoutes.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(state.suggestions) { suggestion ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSuggestion(suggestion)
                                    viewModel.getRoute()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null)
                            Spacer(Modifier.width(8.dp))
                            Text(suggestion.name)
                        }
                    }
                }
            } else if (state.plannedRoutes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.plannedRoutes.size) { index ->
                        val route = state.plannedRoutes[index]
                        val selected = index == state.selectedRouteIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectRoute(index) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Route ${index + 1}", fontWeight = FontWeight.Bold)
                                    Text(RiskCalculator.formatDistance(route.distanceMeters))
                                    Text(RiskCalculator.formatEta(route.etaSeconds))
                                    Text(
                                        "${route.analysis.riskScore}% ${RiskCalculator.riskLabel(route.analysis.riskScore)}",
                                        color = RiskCalculator.riskColor(route.analysis.riskScore)
                                    )
                                    if (route.analysis.riskScore > 50 && index == 0) {
                                        Text(
                                            "High risk — try a safer alternative below",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    RiskBadge(route.analysis.riskScore)
                                    if (selected) Icon(Icons.Default.Check, null)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.getSelectedRoute()?.let { primary ->
                            onRoutePlanned(primary, viewModel.getAlternatives())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Show Route on Map") }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (state.selectedDestination != null) viewModel.getRoute()
                        else viewModel.searchOnEnter()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.destinationQuery.length >= 2
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Get Route")
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
