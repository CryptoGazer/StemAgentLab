package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.core.model.FrozenAgent
import com.stemlab.ui.theme.*

@Composable
fun FrozenAgentPanel(frozenAgent: FrozenAgent?, modifier: Modifier = Modifier) {
    PanelCard(title = "Frozen Agent", modifier = modifier) {
        if (frozenAgent == null) {
            Text(
                "No frozen agent yet — run evolution to produce one.",
                color = OnSurfaceDim,
                fontSize = 12.sp
            )
        } else {
            FrozenAgentBody(frozenAgent)
        }
    }
}

@Composable
private fun FrozenAgentBody(agent: FrozenAgent) {
    // Name + domain header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(agent.config.name, color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(agent.domain, color = OnSurfaceDim, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(SuccessGreen.copy(alpha = 0.18f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text("FROZEN", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(8.dp))

    // Score + improvement row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FrozenMetric("Score", "%.3f".format(agent.score), SuccessGreen)
        FrozenMetric(
            "vs Baseline",
            "${if (agent.improvementPercent >= 0) "+" else ""}${"%.1f".format(agent.improvementPercent)}%",
            if (agent.improvementPercent > 0) SuccessGreen else WarningAmber
        )
        FrozenMetric("Tokens", "${agent.estimatedTokens}", AccentCyan)
        FrozenMetric("Cost", "\$${"%.4f".format(agent.estimatedCost)}", WarningAmber)
    }

    Spacer(Modifier.height(8.dp))

    // Prompt strategy
    FrozenRow("Strategy", agent.config.promptStrategy)

    // Tools
    if (agent.config.tools.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        FrozenRow("Tools", agent.config.tools.joinToString(" · "))
    }

    // Skills
    if (agent.config.skills.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        FrozenRow("Skills", agent.config.skills.joinToString(" · "))
    }

    // Description
    if (agent.config.description.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(agent.config.description, color = OnSurfaceDim, fontSize = 11.sp)
    }

    Spacer(Modifier.height(6.dp))

    // Frozen timestamp
    val shortTs = agent.frozenAt.take(19).replace('T', ' ')
    Text("Frozen at: $shortTs", color = OnSurfaceDim, fontSize = 10.sp)

    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = SurfaceVariant)
    Spacer(Modifier.height(6.dp))

    // Saved artifact path — selectable for copy/paste
    Text("Saved to:", color = OnSurfaceDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(2.dp))
    SelectionContainer {
        Text(
            "projects/${agent.projectId}/agent.json",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FrozenMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = OnSurfaceDim, fontSize = 10.sp)
    }
}

@Composable
private fun FrozenRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = OnSurfaceDim, fontSize = 11.sp, modifier = Modifier.width(54.dp))
        Text(value, color = OnSurface, fontSize = 11.sp)
    }
}
