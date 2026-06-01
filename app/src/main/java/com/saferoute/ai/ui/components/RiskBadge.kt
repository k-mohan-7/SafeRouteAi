package com.saferoute.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.domain.model.RiskCalculator

@Composable
fun RiskBadge(score: Int, modifier: Modifier = Modifier) {
    val color = RiskCalculator.riskColor(score)
    val label = RiskCalculator.riskLabel(score)
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("$score  $label", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
