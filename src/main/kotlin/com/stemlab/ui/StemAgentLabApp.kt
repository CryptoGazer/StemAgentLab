package com.stemlab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.text.selection.SelectionContainer
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
    val activeProject = state.activeProject

    var showResetDialog by remember { mutableStateOf(false) }
    var resetInput by remember { mutableStateOf("") }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<com.stemlab.app.ProjectViewState?>(null) }
    var rightPaneHeightPx by remember { mutableStateOf(1) }
    var candidatePaneFraction by remember { mutableStateOf(0.42f) }

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = SurfaceDark) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val outerScrollState = rememberScrollState()
                val mainContentHeight = (maxHeight - 156.dp).coerceAtLeast(720.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(outerScrollState)
                        .padding(16.dp),
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
                            ProjectSelector(
                                projects = state.projects,
                                activeProjectId = state.activeProjectId,
                                onSelect = controller::selectProject,
                                onNewProject = { showNewProjectDialog = true }
                            )
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
                        Text(activeProject?.statusMessage ?: "Create a project to begin.", color = OnSurfaceDim, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(state.llmLabel, color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            when {
                                activeProject?.isRunning == true -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentCyan
                                    )
                                    Text(
                                        activeProject.currentPhase.name.replace("_", " "),
                                        color = AccentCyan,
                                        fontSize = 11.sp
                                    )
                                }
                                activeProject?.currentPhase == Phase.DONE ->
                                    Text("DONE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                activeProject?.currentPhase == Phase.IDLE && activeProject.candidates.isNotEmpty() ->
                                    Text("STOPPED", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                else -> {}
                            }
                        }
                    }

                    // ─── Action buttons ───────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeProject?.isRunning == true) {
                            Button(
                                onClick = controller::stopEvolution,
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("■  Stop Evolution", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            repeat(4) {
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
                                enabled = activeProject != null,
                                primary = true,
                                onClick = controller::runEvolution,
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                label = "✓  Final Evaluation",
                                enabled = activeProject?.lastResult != null,
                                primary = false,
                                onClick = controller::runFinalEvaluation,
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                label = "⟳  Eval Frozen",
                                enabled = activeProject?.frozenAgent != null,
                                primary = false,
                                onClick = controller::evaluateFrozenAgent,
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                label = "↓  Export Report",
                                enabled = activeProject?.lastResult != null,
                                primary = false,
                                onClick = controller::exportReport,
                                modifier = Modifier.weight(1f)
                            )
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
                        modifier = Modifier.fillMaxWidth().height(mainContentHeight),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left column
                        Column(
                            modifier = Modifier.width(300.dp).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProjectMenuPanel(
                                projects = state.projects,
                                activeProjectId = state.activeProjectId,
                                onSelect = controller::selectProject,
                                onNewProject = { showNewProjectDialog = true },
                                onDeleteProject = { projectToDelete = activeProject },
                                modifier = Modifier.height(220.dp)
                            )
                            if (activeProject != null) {
                                SettingsPanel(
                                    projectName = activeProject.name,
                                    domain = activeProject.domain,
                                    description = activeProject.spec.description,
                                    isRunning = activeProject.isRunning,
                                    onApply = controller::updateActiveProject
                                )
                                MetricsPanel(metrics = activeProject.metrics)
                                OutputFilesPanel(
                                    frozenAgentPath = activeProject.frozenAgent?.let { "projects/${it.projectId}/agent.json" },
                                    reportPath = activeProject.lastExportPath
                                )
                                ToolRegistryPanel(tools = activeProject.selectedTools, modifier = Modifier.weight(0.66f))
                            }
                        }

                        // Right column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onSizeChanged { rightPaneHeightPx = it.height.coerceAtLeast(1) },
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CandidateListPanel(
                                candidates = activeProject?.let { project ->
                                    project.candidates.filterNot { it.id in project.dismissedCandidateIds }
                                }.orEmpty(),
                                onDismissRejected = controller::dismissRejectedCandidate,
                                modifier = Modifier.weight(candidatePaneFraction)
                            )
                            VerticalResizeHandle(
                                onDrag = { deltaY ->
                                    val deltaFraction = deltaY / rightPaneHeightPx.toFloat()
                                    candidatePaneFraction = (candidatePaneFraction + deltaFraction).coerceIn(0.22f, 0.72f)
                                }
                            )
                            FrozenAgentPanel(
                                frozenAgent = activeProject?.frozenAgent,
                                modifier = Modifier.height(190.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            EvolutionLogPanel(
                                logs = activeProject?.logs.orEmpty(),
                                modifier = Modifier.weight(1f - candidatePaneFraction)
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(outerScrollState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )

                // ─── In-app dialog overlays (always on top, no OS-level JDialog) ───
                if (showResetDialog) {
                    ResetConfirmOverlay(
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

                if (showNewProjectDialog) {
                    NewProjectOverlay(
                        onCreate = { name, domain, description ->
                            controller.createProject(name, domain, description)
                            showNewProjectDialog = false
                        },
                        onDismiss = { showNewProjectDialog = false }
                    )
                }

                val deletingProject = projectToDelete
                if (deletingProject != null) {
                    DeleteProjectOverlay(
                        projectName = deletingProject.name,
                        onConfirm = {
                            controller.deleteProject(deletingProject.id)
                            projectToDelete = null
                        },
                        onDismiss = { projectToDelete = null }
                    )
                }
            }
        }
    }
}

// ─── In-app overlay dialogs ─────────────────────────────────────────────────

@Composable
private fun OverlayScrim(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Semi-transparent scrim — clicks on it dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        )
        // Dialog card — sits on top, absorbs its own clicks so they don't reach the scrim
        content()
    }
}

@Composable
private fun DeleteProjectOverlay(
    projectName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    OverlayScrim(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .clickable {}   // absorb clicks so they don't reach the scrim
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Delete Project?", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Delete \"$projectName\" and its local runs/reports?",
                color = OnSurface, fontSize = 13.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, SurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Cancel", color = OnSurfaceDim) }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ResetConfirmOverlay(
    input: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    OverlayScrim(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .clickable {}
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Reset All Progress?", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "This will delete all run history, cached results, and exported reports. " +
                "This cannot be undone.",
                color = OnSurface, fontSize = 13.sp
            )
            Text("Type RESET to confirm:", color = OnSurfaceDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, SurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Cancel", color = OnSurfaceDim) }
                Button(
                    onClick = onConfirm,
                    enabled = input.trim() == "RESET",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        disabledContainerColor = SurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Delete Everything", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun NewProjectOverlay(
    onCreate: (name: String, domain: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val canCreate = domain.trim().isNotBlank()

    OverlayScrim(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .clickable {}
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("New Project", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            DialogField(value = name, onValueChange = { name = it }, label = "Name", placeholder = "e.g. SQL Review")
            DialogField(value = domain, onValueChange = { domain = it }, label = "Domain", placeholder = "e.g. SQL Optimizer")
            DialogField(value = description, onValueChange = { description = it }, label = "Description", placeholder = "Optional project notes")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, SurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Cancel", color = OnSurfaceDim) }
                Button(
                    onClick = { onCreate(name, domain, description) },
                    enabled = canCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─── Reusable components ─────────────────────────────────────────────────────

@Composable
private fun VerticalResizeHandle(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceVariant.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(OnSurfaceDim.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun ProjectSelector(
    projects: List<com.stemlab.app.ProjectViewState>,
    activeProjectId: String?,
    onSelect: (String) -> Unit,
    onNewProject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val activeProject = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
        ) {
            Text(activeProject?.name ?: "Projects", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CardBackground
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(project.name, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(project.domain, color = OnSurfaceDim, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelect(project.id)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(color = SurfaceVariant)
            DropdownMenuItem(
                text = { Text("+ New Project", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                onClick = {
                    expanded = false
                    onNewProject()
                }
            )
        }
    }
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = OnSurfaceDim) },
        placeholder = { Text(placeholder, color = OnSurfaceDim) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = SurfaceVariant,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            cursorColor = AccentCyan
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProjectMenuPanel(
    projects: List<com.stemlab.app.ProjectViewState>,
    activeProjectId: String?,
    onSelect: (String) -> Unit,
    onNewProject: () -> Unit,
    onDeleteProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(title = "Projects", modifier = modifier) {
        Button(
            onClick = onNewProject,
            modifier = Modifier.fillMaxWidth().height(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+ New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onDeleteProject,
            enabled = activeProjectId != null,
            modifier = Modifier.fillMaxWidth().height(30.dp),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ErrorRed,
                disabledContentColor = OnSurfaceDim
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Delete Selected", fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize().padding(end = 8.dp)
            ) {
                items(projects) { project ->
                    ProjectRow(
                        project = project,
                        selected = project.id == activeProjectId,
                        onClick = { onSelect(project.id) }
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ProjectRow(
    project: com.stemlab.app.ProjectViewState,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> PrimaryGreen
        project.isRunning -> AccentCyan
        else -> SurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) PrimaryGreenVariant.copy(alpha = 0.22f) else SurfaceVariant)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(project.name, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (project.isRunning) {
                Text("RUNNING", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(project.domain, color = OnSurfaceDim, fontSize = 11.sp)
    }
}

@Composable
private fun OutputFilesPanel(
    frozenAgentPath: String?,
    reportPath: String?,
    modifier: Modifier = Modifier
) {
    if (frozenAgentPath == null && reportPath == null) return
    PanelCard(title = "Output Files", modifier = modifier) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (frozenAgentPath != null) {
                    OutputFileRow(label = "Frozen agent", path = frozenAgentPath)
                }
                if (reportPath != null) {
                    OutputFileRow(label = "Report", path = reportPath)
                }
            }
        }
    }
}

@Composable
private fun OutputFileRow(label: String, path: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = OnSurfaceDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            path,
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
                contentColor = Color.Black,
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
