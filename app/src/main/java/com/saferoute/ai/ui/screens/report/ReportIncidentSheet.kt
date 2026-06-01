package com.saferoute.ai.ui.screens.report

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.saferoute.ai.ui.components.ReportMapPicker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportIncidentSheet(
    initialLat: Double? = null,
    initialLng: Double? = null,
    locationLabel: String = "My Location",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialLat, initialLng, locationLabel) {
        if (initialLat != null && initialLng != null) {
            viewModel.setGpsLocation(initialLat, initialLng, locationLabel)
        }
    }
    LaunchedEffect(state.success) {
        if (state.success) {
            viewModel.reset()
            onSuccess()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.setImage(uri) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
                Text("Report Incident", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.incidentTypes.forEach { type ->
                        FilterChip(
                            selected = state.incidentType == type,
                            onClick = { viewModel.updateType(type) },
                            label = { Text(type) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    supportingText = { Text("${state.description.length}/1000") }
                )
                Spacer(Modifier.height(8.dp))
                Text("Location: ${state.locationLabel}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (initialLat != null && initialLng != null) {
                                viewModel.useCurrentGps(initialLat, initialLng, locationLabel)
                            }
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, null)
                        Text("Use GPS")
                    }
                    OutlinedButton(onClick = { viewModel.toggleMapPicker(!state.showMapPicker) }) {
                        Icon(Icons.Default.Place, null)
                        Text("Pin on Map")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("How severe is this incident?")
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { viewModel.updateSeverity(star) }) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (star <= state.severity) Color(0xFFD97706) else Color.LightGray
                            )
                        }
                    }
                }
                OutlinedButton(onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Attach Photo") }
            state.imageUri?.let {
                AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(120.dp), contentScale = ContentScale.Crop)
            }

            if (state.showMapPicker && initialLat != null && initialLng != null) {
                Spacer(Modifier.height(8.dp))
                Text("Long-press on the map to drop a pin", style = MaterialTheme.typography.labelMedium)
                ReportMapPicker(
                    initialLat = initialLat,
                    initialLng = initialLng,
                    onLocationPicked = viewModel::updateCustomLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isSubmitting && state.latitude != null
            ) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Submit")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
