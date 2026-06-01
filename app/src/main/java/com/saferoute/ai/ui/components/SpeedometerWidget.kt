package com.saferoute.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.ui.theme.AmberPrimary
import com.saferoute.ai.ui.theme.BrownSecondary
import com.saferoute.ai.ui.theme.DarkSurface
import kotlin.math.min

@Composable
fun SpeedometerWidget(speedKmh: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier.size(100.dp).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val sweep = min(speedKmh / 120f, 1f) * 270f
            Canvas(modifier = Modifier.size(84.dp)) {
                val stroke = 8f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = BrownSecondary.copy(alpha = 0.4f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = AmberPrimary,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${speedKmh.toInt()}",
                    color = AmberPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text("km/h", color = AmberPrimary.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
    }
}
