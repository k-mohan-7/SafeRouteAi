package com.saferoute.ai.ui.components

import android.graphics.Paint
import com.saferoute.ai.domain.model.RiskCalculator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

fun drawSegmentedPolyline(
    mapView: MapView,
    points: List<GeoPoint>,
    riskScores: List<Int>,
    strokeWidth: Float = 14f,
    fallbackRiskScore: Int = 0,
    dimmed: Boolean = false
) {
    points.zipWithNext().forEachIndexed { i, (a, b) ->
        val score = riskScores.getOrElse(i) { fallbackRiskScore }.let {
            if (it <= 0) fallbackRiskScore else it
        }
        val line = Polyline(mapView).apply {
            setPoints(listOf(a, b))
            outlinePaint.color = if (dimmed) {
                android.graphics.Color.argb(140, 150, 150, 200)
            } else {
                riskScoreToAndroidColor(score)
            }
            outlinePaint.strokeWidth = strokeWidth
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        mapView.overlays.add(line)
    }
    mapView.invalidate()
}

fun riskScoreToAndroidColor(score: Int): Int {
    val c = RiskCalculator.riskColor(score)
    return android.graphics.Color.argb(
        (c.alpha * 255).toInt(),
        (c.red * 255).toInt(),
        (c.green * 255).toInt(),
        (c.blue * 255).toInt()
    )
}
