package com.example.odmas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.odmas.core.agents.RiskLevel

@Composable
fun StatusChip(
    riskLevel: RiskLevel,
    isEscalated: Boolean,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon, text) = getStatusInfo(riskLevel, isEscalated)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun getStatusInfo(
    riskLevel: RiskLevel,
    isEscalated: Boolean
): StatusInfo {
    return when {
        isEscalated -> StatusInfo(
            backgroundColor = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.onError,
            icon = Icons.Default.Warning,
            text = "VERIFY REQUIRED"
        )
        riskLevel == RiskLevel.CRITICAL -> StatusInfo(
            backgroundColor = Color(0xFFD32F2F),
            textColor = Color.White,
            icon = Icons.Default.Warning,
            text = "CRITICAL RISK"
        )
        riskLevel == RiskLevel.HIGH -> StatusInfo(
            backgroundColor = Color(0xFFFF5722),
            textColor = Color.White,
            icon = Icons.Default.Warning,
            text = "HIGH RISK"
        )
        riskLevel == RiskLevel.MEDIUM -> StatusInfo(
            backgroundColor = Color(0xFFFF9800),
            textColor = Color.White,
            icon = Icons.Default.Info,
            text = "MEDIUM RISK"
        )
        else -> StatusInfo(
            backgroundColor = Color(0xFF4CAF50),
            textColor = Color.White,
            icon = Icons.Default.CheckCircle,
            text = "ALL GOOD"
        )
    }
}

private data class StatusInfo(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)
