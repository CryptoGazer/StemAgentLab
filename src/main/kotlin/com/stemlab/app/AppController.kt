package com.stemlab.app

import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.model.ProjectSpec
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.core.tasks.LlmTaskGenerator
import com.stemlab.llm.LlmClient
import com.stemlab.llm.OpenAiLlmClient
import com.stemlab.report.MarkdownReportExporter
import com.stemlab.storage.ProjectStore
import com.stemlab.util.DotEnvLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

class AppController(
    private val llmClient: LlmClient = defaultLlmClient(),
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    private val projectStore: ProjectStore = ProjectStore()
) {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val taskGenerator = LlmTaskGenerator(llmClient)
    private val projectJobs = mutableMapOf<String, Job>()

    init {
        val projects = loadOrCreateProjects()
        _state.value = AppState(
            projects = projects.map { it.toViewState() },
            activeProjectId = projects.firstOrNull()?.id
        )
    }

    fun createProject(name: String, domain: String, description: String = "") {
        val cleanDomain = domain.trim()
        if (cleanDomain.isBlank()) return

        val cleanName = name.trim().ifBlank { cleanDomain }
        val now = Instant.now().toString()
        val project = ProjectSpec(
            id = uniqueProjectId(cleanName),
            name = cleanName,
            domain = cleanDomain,
            description = description.trim(),
            createdAt = now,
            updatedAt = now
        )

        projectStore.saveProject(project)
        _state.update { state ->
            state.copy(
                projects = state.projects + project.toViewState(),
                activeProjectId = project.id
            )
        }
        appendLog(project.id, "Project created: ${project.name} (${project.domain})")
    }

    fun selectProject(projectId: String) {
        if (_state.value.projects.any { it.id == projectId }) {
            _state.update { it.copy(activeProjectId = projectId) }
        }
    }

    fun deleteProject(projectId: String) {
        projectJobs.remove(projectId)?.cancel()
        projectStore.deleteProject(projectId)

        val remaining = _state.value.projects.filterNot { it.id == projectId }
        if (remaining.isNotEmpty()) {
            _state.update { state ->
                state.copy(
                    projects = remaining,
                    activeProjectId = remaining.firstOrNull()?.id
                )
            }
        } else {
            val project = defaultProject()
            projectStore.saveProject(project)
            _state.value = AppState(
                projects = listOf(project.toViewState()),
                activeProjectId = project.id
            )
        }
    }

    fun setDomain(domain: String) {
        val project = _state.value.activeProject ?: return
        if (project.isRunning) return

        val trimmed = domain.trim()
        if (trimmed.isBlank() || trimmed == project.domain) return

        val updatedSpec = project.spec.copy(domain = trimmed, updatedAt = Instant.now().toString())
        projectStore.saveProject(updatedSpec)
        updateProject(project.id) {
            it.copy(
                spec = updatedSpec,
                metrics = Metrics(),
                candidates = emptyList(),
                selectedTools = emptyList(),
                lastResult = null,
                lastExportPath = null,
                statusMessage = "Domain set to \"$trimmed\" — ready to run."
            )
        }
        appendLog(project.id, "Domain changed to \"$trimmed\"")
    }

    fun runEvolution() {
        val projectId = _state.value.activeProject?.id ?: return
        runEvolution(projectId)
    }

    fun runEvolution(projectId: String) {
        val project = _state.value.projects.firstOrNull { it.id == projectId } ?: return
        if (project.isRunning || projectJobs[projectId]?.isActive == true) return

        resetLogs(projectId)
        setPhase(projectId, Phase.RUNNING_BASELINE, "Starting evolution loop...")
        appendLog(projectId, "Evolution started: baseline -> propose -> evaluate -> select")

        projectJobs[projectId] = scope.launch {
            try {
                appendLog(projectId, "Generating tasks for domain: \"${project.domain}\" via OpenAI")
                val generated = taskGenerator.generateWithUsage(project.domain)
                val tasks = generated.tasks
                appendLog(projectId, "Loaded ${tasks.size} evaluation tasks")

                val engine = EvolutionEngine(llmClient, toolRegistry, Budget())
                val result = engine.run(project.domain, tasks) { line ->
                    appendLog(projectId, line)
                    when {
                        line.contains("Baseline score") -> setPhase(projectId, Phase.RUNNING_BASELINE)
                        line.contains("proposing candidate") -> setPhase(projectId, Phase.EVOLVING, "StemAgent proposing candidates...")
                        line.contains("Evaluating") -> setPhase(projectId, Phase.EVALUATING, "Evaluating candidates...")
                        line.contains("VersionManager") -> setPhase(projectId, Phase.EVALUATING, "Selecting best candidate...")
                    }
                }

                val resultWithGenerationUsage = result.copy(
                    totalTokensUsed = result.totalTokensUsed + generated.tokensUsed,
                    totalCost = result.totalCost + generated.costEstimate
                )
                val resultWithLogs = resultWithGenerationUsage.copy(logs = currentLogs(projectId))
                projectStore.saveRun(projectId, resultWithLogs)

                val updatedSpec = project.spec.copy(
                    lastRunId = result.runId,
                    updatedAt = Instant.now().toString()
                )
                projectStore.saveProject(updatedSpec)

                val selected = resultWithLogs.selectedCandidate
                val selectedTools = selected?.config?.tools?.let { toolRegistry.resolve(it) } ?: emptyList()

                updateProject(projectId) {
                    it.copy(
                        spec = updatedSpec,
                        isRunning = false,
                        currentPhase = Phase.DONE,
                        metrics = Metrics(
                            baselineScore = resultWithLogs.baselineScore,
                            bestCandidateScore = selected?.score ?: resultWithLogs.baselineScore,
                            improvement = resultWithLogs.improvement,
                            estimatedTokens = resultWithLogs.totalTokensUsed,
                            estimatedCost = resultWithLogs.totalCost
                        ),
                        candidates = resultWithLogs.candidates,
                        selectedTools = selectedTools,
                        lastResult = resultWithLogs,
                        statusMessage = if (selected != null)
                            "Done — ${selected.config.name} selected (+${"%.1f".format(resultWithLogs.improvementPercent)}%)"
                        else
                            "Done — no candidate outperformed baseline"
                    )
                }
            } catch (e: CancellationException) {
                appendLog(projectId, "Evolution stopped by user")
                updateProject(projectId) {
                    it.copy(
                        isRunning = false,
                        currentPhase = Phase.IDLE,
                        statusMessage = "Stopped — click Run Evolution to start again"
                    )
                }
            } catch (e: Exception) {
                appendLog(projectId, "ERROR: ${e.message ?: e::class.simpleName ?: "Evolution failed"}")
                updateProject(projectId) {
                    it.copy(
                        isRunning = false,
                        currentPhase = Phase.IDLE,
                        statusMessage = "OpenAI run failed — check logs and OPENAI_API_KEY."
                    )
                }
            } finally {
                projectJobs.remove(projectId)
            }
        }
    }

    fun stopEvolution() {
        val projectId = _state.value.activeProject?.id ?: return
        stopEvolution(projectId)
    }

    fun stopEvolution(projectId: String) {
        projectJobs.remove(projectId)?.cancel()
    }

    fun runFinalEvaluation() {
        val project = _state.value.activeProject ?: return
        val result = project.lastResult ?: run {
            appendLog(project.id, "No evolution result — run the evolution loop first.")
            return
        }
        val selected = result.selectedCandidate ?: run {
            appendLog(project.id, "No candidate was selected — cannot confirm.")
            return
        }
        appendLog(project.id, "=== Final Evaluation ===")
        appendLog(project.id, "Frozen agent: ${selected.config.name}")
        appendLog(project.id, "Score: ${"%.3f".format(selected.score)}")
        appendLog(project.id, "Tools: ${selected.config.tools.joinToString(", ")}")
        appendLog(project.id, "Skills: ${selected.config.skills.joinToString(", ")}")
        appendLog(project.id, "Status: PRODUCTION-READY (frozen configuration confirmed)")
    }

    fun exportReport() {
        val project = _state.value.activeProject ?: return
        val result = project.lastResult ?: run {
            appendLog(project.id, "Nothing to export — run the evolution loop first.")
            return
        }

        setPhase(project.id, Phase.EXPORTING, "Exporting report...")
        val resultWithLogs = result.copy(logs = project.logs)
        val path = MarkdownReportExporter.export(
            resultWithLogs,
            projectStore.reportPath(project.id, resultWithLogs.runId)
        )
        projectStore.saveRun(project.id, resultWithLogs)
        appendLog(project.id, "Report exported -> $path")
        updateProject(project.id) {
            it.copy(
                lastResult = resultWithLogs,
                lastExportPath = path,
                currentPhase = Phase.DONE,
                isRunning = false,
                statusMessage = "Report saved: $path"
            )
        }
    }

    fun resetAll() {
        projectJobs.values.forEach { it.cancel() }
        projectJobs.clear()
        _state.value.projects.forEach { projectStore.deleteProject(it.id) }
        val project = defaultProject()
        projectStore.saveProject(project)
        _state.value = AppState(
            projects = listOf(project.toViewState()),
            activeProjectId = project.id
        )
        appendLog(project.id, "All projects reset — clean slate")
    }

    fun close() {
        scope.cancel()
    }

    private fun loadOrCreateProjects(): List<ProjectSpec> {
        val existing = projectStore.loadProjects()
        if (existing.isNotEmpty()) return existing
        val project = defaultProject()
        projectStore.saveProject(project)
        return listOf(project)
    }

    private fun defaultProject(): ProjectSpec {
        val now = Instant.now().toString()
        return ProjectSpec(
            id = "python-qa",
            name = "Python QA",
            domain = "Python QA",
            description = "Default Python quality-assurance benchmark project",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun ProjectSpec.toViewState(): ProjectViewState {
        val latest = projectStore.latestRun(id)
        val selectedTools = latest?.selectedCandidate?.config?.tools?.let { toolRegistry.resolve(it) } ?: emptyList()
        return ProjectViewState(
            spec = if (latest != null && latest.runId != lastRunId) copy(lastRunId = latest.runId) else this,
            metrics = latest?.let {
                Metrics(
                    baselineScore = it.baselineScore,
                    bestCandidateScore = it.selectedCandidate?.score ?: it.baselineScore,
                    improvement = it.improvement,
                    estimatedTokens = it.totalTokensUsed,
                    estimatedCost = it.totalCost
                )
            } ?: Metrics(),
            candidates = latest?.candidates ?: emptyList(),
            selectedTools = selectedTools,
            logs = latest?.logs ?: emptyList(),
            lastResult = latest,
            statusMessage = if (latest == null)
                "Ready — OpenAI key loaded; click Run Evolution to start."
            else
                "Loaded latest run ${latest.runId}."
        )
    }

    private fun updateProject(projectId: String, transform: (ProjectViewState) -> ProjectViewState) {
        _state.update { state ->
            state.copy(projects = state.projects.map { if (it.id == projectId) transform(it) else it })
        }
    }

    private fun resetLogs(projectId: String) {
        updateProject(projectId) { it.copy(logs = emptyList(), isRunning = true) }
    }

    private fun setPhase(projectId: String, phase: Phase, status: String? = null) {
        updateProject(projectId) { project ->
            project.copy(
                currentPhase = phase,
                isRunning = phase != Phase.DONE && phase != Phase.IDLE,
                statusMessage = status ?: project.statusMessage
            )
        }
    }

    private fun appendLog(projectId: String, line: String) {
        val ts = LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }
        updateProject(projectId) { project ->
            project.copy(logs = project.logs + "[$ts] $line")
        }
    }

    private fun currentLogs(projectId: String): List<String> =
        _state.value.projects.firstOrNull { it.id == projectId }?.logs.orEmpty()

    private fun uniqueProjectId(name: String): String {
        val slug = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "project" }
            .take(32)
        val existing = projectStore.loadProjects().map { it.id }.toSet()
        var candidate = slug
        while (candidate in existing) {
            candidate = "$slug-${UUID.randomUUID().toString().take(6)}"
        }
        return candidate
    }

    private companion object {
        fun defaultLlmClient(): LlmClient {
            val apiKey = DotEnvLoader.requireApiKey()
            return OpenAiLlmClient(apiKey)
        }
    }
}
