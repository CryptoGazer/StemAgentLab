package com.stemlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun CandidateListPanel(
    candidates: List<CandidateAgent>,
    onDismissRejected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(title = "Candidate Agents", modifier = modifier) {
        if (candidates.isEmpty()) {
            Text("No candidates yet — run the evolution loop.", color = OnSurfaceDim, fontSize = 13.sp)
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates, key = { it.id }) { candidate ->
                        CandidateRow(candidate, onDismissRejected)
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: CandidateAgent,
    onDismissRejected: (String) -> Unit
) {
    val isBaseline = candidate.id == "baseline"
    val borderColor = when (candidate.status) {
        CandidateStatus.SELECTED -> SuccessGreen
        CandidateStatus.EVALUATING -> AccentCyan
        CandidateStatus.REJECTED if isBaseline -> OnSurfaceDim.copy(alpha = 0.35f)
        CandidateStatus.REJECTED -> WarningAmber.copy(alpha = 0.45f)
        CandidateStatus.PENDING -> SurfaceVariant
    }
    val progressColor = when (candidate.status) {
        CandidateStatus.SELECTED -> SuccessGreen
        CandidateStatus.EVALUATING -> AccentCyan
        CandidateStatus.REJECTED if isBaseline -> SecondaryBlue
        CandidateStatus.REJECTED -> WarningAmber
        CandidateStatus.PENDING -> OnSurfaceDim
    }
    val badgeText = when (candidate.status) {
        CandidateStatus.SELECTED -> "✓ SELECTED"
        CandidateStatus.EVALUATING -> "…"
        CandidateStatus.REJECTED if isBaseline -> "BASELINE"
        CandidateStatus.REJECTED -> "✗"
        CandidateStatus.PENDING -> "—"
    }
    val badgeColor = when (candidate.status) {
        CandidateStatus.SELECTED -> SuccessGreen
        CandidateStatus.EVALUATING -> AccentCyan
        CandidateStatus.REJECTED if isBaseline -> SecondaryBlue
        CandidateStatus.REJECTED -> WarningAmber
        CandidateStatus.PENDING -> OnSurfaceDim
    }
    val canDismiss = candidate.status == CandidateStatus.REJECTED && !isBaseline

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(badgeText, color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (canDismiss) {
                    IconButton(
                        onClick = { onDismissRejected(candidate.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide rejected candidate",
                            tint = OnSurfaceDim,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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
                    color = progressColor,
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
