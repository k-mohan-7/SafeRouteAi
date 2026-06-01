package com.saferoute.ai.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.saferoute.ai.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                state.profile?.let { p ->
                    Text(p.fullname, style = MaterialTheme.typography.headlineMedium)
                    Text(p.email)
                    p.phone?.let { Text(it) }
                    Spacer(Modifier.height(8.dp))
                    Text("Reports made: ${state.reportCount}")
                    Text("Saved routes: ${state.routeHistory.size}")
                }
                Spacer(Modifier.height(16.dp))
                Text("Route History", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(state.routeHistory) { route ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                route.destinationName ?: "Route #${route.id}",
                                modifier = Modifier.padding(12.dp)
                            )
                            Text(
                                TimeUtils.timeAgo(route.travelDate),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Logout") }
            }
        }
    }
}
