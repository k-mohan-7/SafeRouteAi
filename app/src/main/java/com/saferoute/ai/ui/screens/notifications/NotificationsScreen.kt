package com.saferoute.ai.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.saferoute.ai.ui.components.EmptyState
import com.saferoute.ai.ui.theme.AmberPrimary
import com.saferoute.ai.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToMap: (Double?, Double?) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }) }) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
        } else if (state.notifications.isEmpty()) {
            EmptyState("No notifications")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.notifications, key = { it.id }) { n ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        onClick = { onNavigateToMap(n.latitude, n.longitude) }
                    ) {
                        Box {
                            if (!n.isRead) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.02f)
                                        .matchParentSize()
                                        .background(AmberPrimary)
                                )
                            }
                            Text(
                                "${n.message}\n${TimeUtils.timeAgo(n.createdAt)}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
