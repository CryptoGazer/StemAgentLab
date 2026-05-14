package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.app.Metrics
import com.stemlab.ui.theme.*

@Composable
fun MetricsPanel(metrics: Metrics, modifier: Modifier = Modifier) {
    PanelCard(title = "Metrics", modifier = modifier) {
        MetricRow("Baseline score", "%.3f".format(metrics.baselineScore), OnSurfaceDim)
        MetricRow(
            "Best candidate",
            "%.3f".format(metrics.bestCandidateScore),
            if (metrics.bestCandidateScore > metrics.baselineScore) SuccessGreen else OnSurface
        )
        MetricRow(
            "Improvement",
            if (metrics.improvementPercent > 0) "+%.1f%%".format(metrics.improvementPercent) else "—",
            if (metrics.improvementPercent > 0) SuccessGreen else OnSurfaceDim
        )
        MetricRow("Est. tokens", if (metrics.estimatedTokens > 0) "${metrics.estimatedTokens}" else "—", AccentCyan)
        MetricRow(
            "Est. cost",
            if (metrics.estimatedCost > 0) "\$%.4f".format(metrics.estimatedCost) else "—",
            WarningAmber
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OnSurfaceDim, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PanelCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .padding(14.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = OnSurfaceDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
