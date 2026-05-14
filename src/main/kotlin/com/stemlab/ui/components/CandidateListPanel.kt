package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.CandidateStatus
import com.stemlab.ui.theme.*

@Composable
fun CandidateListPanel(candidates: List<CandidateAgent>, modifier: Modifier = Modifier) {
    PanelCard(title = "Candidate Agents", modifier = modifier) {
        if (candidates.isEmpty()) {
            Text("No candidates yet — run the evolution loop.", color = OnSurfaceDim, fontSize = 13.sp)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates) { candidate ->
                    CandidateRow(candidate)
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: CandidateAgent) {
    val borderColor = when (candidate.status) {
        CandidateStatus.SELECTED -> SuccessGreen
        CandidateStatus.EVALUATING -> AccentCyan
        CandidateStatus.REJECTED -> SurfaceVariant
        CandidateStatus.PENDING -> SurfaceVariant
    }
    val badgeText = when (candidate.status) {
        CandidateStatus.SELECTED -> "✓ SELECTED"
        CandidateStatus.EVALUATING -> "…"
        CandidateStatus.REJECTED -> "✗"
        CandidateStatus.PENDING -> "—"
    }
    val badgeColor = when (candidate.status) {
        CandidateStatus.SELECTED -> SuccessGreen
        CandidateStatus.EVALUATING -> AccentCyan
        else -> OnSurfaceDim
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(candidate.config.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(badgeText, color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (candidate.score > 0.0) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { candidate.score.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = borderColor,
                    trackColor = SurfaceDark
                )
                Spacer(Modifier.width(10.dp))
                Text("%.3f".format(candidate.score), color = OnSurface, fontSize = 12.sp)
            }
        }

        if (candidate.config.tools.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                candidate.config.tools.joinToString(" · "),
                color = OnSurfaceDim,
                fontSize = 11.sp
            )
        }
    }
}
