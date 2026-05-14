package com.stemlab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemlab.app.AppController
import com.stemlab.app.Phase
import com.stemlab.ui.components.*
import com.stemlab.ui.theme.*

@Composable
fun StemAgentLabApp(controller: AppController, onQuit: () -> Unit = {}) {
    val state by controller.state.collectAsState()

    // Reset confirmation dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetInput by remember { mutableStateOf("") }

    if (showResetDialog) {
        ResetConfirmDialog(
            input = resetInput,
            onInputChange = { resetInput = it.uppercase() },
            onConfirm = {
                controller.resetAll()
                showResetDialog = false
                resetInput = ""
            },
            onDismiss = {
                showResetDialog = false
                resetInput = ""
            }
        )
    }

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = SurfaceDark) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── Header ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stem Agent Lab", color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Controlled agent specialisation loop", color = OnSurfaceDim, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DomainBadge(state.domain)
                        ModeBadge()
                    }
                }

                // ─── Status bar ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardBackground)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(state.statusMessage, color = OnSurfaceDim, fontSize = 12.sp)
                    when {
                        state.isRunning -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentCyan
                            )
                            Text(
                                state.currentPhase.name.replace("_", " "),
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                        state.currentPhase == Phase.DONE ->
                            Text("● DONE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        state.currentPhase == Phase.IDLE && state.candidates.isNotEmpty() ->
                            Text("● STOPPED", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        else -> {}
                    }
                }

                // ─── Action buttons ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isRunning) {
                        // While running: show only Stop prominently
                        Button(
                            onClick = controller::stopEvolution,
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("■  Stop Evolution", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        // Grayed-out placeholders so layout doesn't jump
                        repeat(3) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("—", fontSize = 13.sp) }
                        }
                    } else {
                        ActionButton(
                            label = "▶  Run Evolution",
                            enabled = true,
                            primary = true,
                            onClick = controller::runEvolution,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            label = "✓  Final Evaluation",
                            enabled = state.lastResult != null,
                            primary = false,
                            onClick = controller::runFinalEvaluation,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            label = "↓  Export Report",
                            enabled = state.lastResult != null,
                            primary = false,
                            onClick = controller::exportReport,
                            modifier = Modifier.weight(1f)
                        )
                        // Destructive reset — separate, visually distinct
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Text("✕  Reset All", fontSize = 13.sp)
                        }
                    }
                }

                // ─── Main content ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left column: settings + metrics + tools
                    Column(
                        modifier = Modifier.width(240.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsPanel(
                            domain = state.domain,
                            isRunning = state.isRunning,
                            onDomainApply = controller::setDomain
                        )
                        MetricsPanel(metrics = state.metrics)
                        ToolRegistryPanel(tools = state.selectedTools, modifier = Modifier.weight(1f))
                    }

                    // Right column: candidates + log
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CandidateListPanel(candidates = state.candidates, modifier = Modifier.weight(0.42f))
                        EvolutionLogPanel(logs = state.logs, modifier = Modifier.weight(0.58f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetConfirmDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("Reset All Progress?", color = ErrorRed, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This will delete all run history, cached results, and exported reports. " +
                    "This cannot be undone.",
                    color = OnSurface,
                    fontSize = 13.sp
                )
                Text(
                    "Type RESET to confirm:",
                    color = OnSurfaceDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    placeholder = { Text("RESET", color = OnSurfaceDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ErrorRed,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = ErrorRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = input.trim() == "RESET",
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    disabledContainerColor = SurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete Everything", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, SurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = OnSurfaceDim)
            }
        }
    )
}

@Composable
private fun DomainBadge(domain: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PrimaryGreenVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(domain, color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModeBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WarningAmber.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text("OPENAI", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = androidx.compose.ui.graphics.Color.Black,
                disabledContainerColor = SurfaceVariant,
                disabledContentColor = OnSurfaceDim
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(40.dp),
            border = BorderStroke(
                1.dp,
                if (enabled) PrimaryGreen.copy(alpha = 0.5f) else SurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (enabled) PrimaryGreen else OnSurfaceDim,
                disabledContentColor = OnSurfaceDim
            )
        ) {
            Text(label, fontSize = 13.sp)
        }
    }
}
