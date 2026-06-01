package com.saferoute.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.saferoute.ai.BuildConfig
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.RiskCalculator
import com.saferoute.ai.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentCard(
    incident: Incident,
    modifier: Modifier = Modifier,
    distanceText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        IncidentCardContent(incident, distanceText, trailing)
    }
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            onClick = onClick
        ) { content() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) { content() }
    }
}

@Composable
private fun IncidentCardContent(
    incident: Incident,
    distanceText: String?,
    trailing: @Composable (() -> Unit)?
) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(RiskCalculator.severityColor(incident.severity))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    incident.incidentType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    incident.description.orEmpty(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        incident.reporter ?: "Anonymous",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        TimeUtils.timeAgo(incident.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    distanceText?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (incident.status == "resolved") {
                    Text("Resolved", color = MaterialTheme.colorScheme.primary)
                }
            }
            incident.imagePath?.let { path ->
                AsyncImage(
                    model = BuildConfig.UPLOAD_BASE_URL + path,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            trailing?.invoke()
        }
    }


@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛣️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
