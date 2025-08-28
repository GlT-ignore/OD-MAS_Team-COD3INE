package com.example.odmas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odmas.core.agents.RiskLevel

@Composable
fun RiskDial(
    risk: Double,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val animatedRisk by animateFloatAsState(
        targetValue = risk.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
        label = "risk"
    )
    
    // Get colors and styles outside of Canvas scope
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = getRiskColor(riskLevel)
    val headlineStyle = MaterialTheme.typography.headlineLarge
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - 20.dp.toPx()) / 2
            
            // Background circle
            drawCircle(
                color = surfaceVariantColor,
                radius = radius,
                center = center,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Progress circle
            val sweepAngle = (animatedRisk / 100f) * 360f
            
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Butt),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // No center indicator to avoid overlapping the score text
        }
        
        // Risk value text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${risk.toInt()}",
                style = headlineStyle,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            
            Text(
                text = "Risk",
                style = bodyStyle,
                color = onSurfaceVariantColor
            )
        }
    }
}

@Composable
private fun getRiskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.LOW -> Color(0xFF4CAF50) // Green
        RiskLevel.MEDIUM -> Color(0xFFFF9800) // Orange
        RiskLevel.HIGH -> Color(0xFFFF5722) // Red-Orange
        RiskLevel.CRITICAL -> Color(0xFFD32F2F) // Red
    }
}
