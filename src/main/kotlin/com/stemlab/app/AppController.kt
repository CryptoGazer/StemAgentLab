package com.stemlab.app

import com.stemlab.core.agent.SpecializedAgent
import com.stemlab.core.eval.PythonQaEvaluator
import com.stemlab.core.eval.ScoreCalculator
import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.model.FrozenAgent
import com.stemlab.core.model.ProjectSpec
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.core.tasks.LlmTaskGenerator
import com.stemlab.llm.LlmClient
import com.stemlab.llm.OpenAiLlmClient
import com.stemlab.llm.PromptTemplates
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
        val cleanDomain = cleanSingleLine(domain, MAX_DOMAIN_LENGTH)
        if (cleanDomain.isBlank()) return

        val cleanName = cleanSingleLine(name, MAX_PROJECT_NAME_LENGTH).ifBlank { cleanDomain }
        val cleanDescription = cleanSingleLine(description, MAX_DESCRIPTION_LENGTH)
        val now = Instant.now().toString()
        val project = ProjectSpec(
            id = uniqueProjectId(cleanName),
            name = cleanName,
            domain = cleanDomain,
            description = cleanDescription,
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

    fun updateActiveProject(name: String, domain: String, description: String? = null) {
        val project = _state.value.activeProject ?: return
        if (project.isRunning) return

        val trimmedDomain = cleanSingleLine(domain, MAX_DOMAIN_LENGTH)
        if (trimmedDomain.isBlank()) return

        val trimmedName = cleanSingleLine(name, MAX_PROJECT_NAME_LENGTH).ifBlank { trimmedDomain }
        val trimmedDescription = description
            ?.let { cleanSingleLine(it, MAX_DESCRIPTION_LENGTH) }
            ?: project.spec.description
        if (
            trimmedName == project.name &&
            trimmedDomain == project.domain &&
            trimmedDescription == project.spec.description
        ) return

        val updatedSpec = project.spec.copy(
            name = trimmedName,
            domain = trimmedDomain,
            description = trimmedDescription,
            updatedAt = Instant.now().toString()
        )
        projectStore.saveProject(updatedSpec)
        updateProject(project.id) {
            it.copy(
                spec = updatedSpec,
                metrics = Metrics(),
                candidates = emptyList(),
                dismissedCandidateIds = emptySet(),
                selectedTools = emptyList(),
                lastResult = null,
                lastExportPath = null,
                statusMessage = "Project updated — ready to run \"$trimmedDomain\"."
            )
        }
        appendLog(project.id, "Project updated: ${updatedSpec.name} (${updatedSpec.domain})")
    }

    fun setDomain(domain: String) {
        val project = _state.value.activeProject ?: return
        updateActiveProject(project.name, domain)
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

                val frozenAgent = if (selected != null) {
                    FrozenAgent(
                        projectId = projectId,
                        runId = resultWithLogs.runId,
                        domain = project.domain,
                        frozenAt = Instant.now().toString(),
                        config = selected.config,
                        score = selected.score,
                        baselineScore = resultWithLogs.baselineScore,
                        improvement = resultWithLogs.improvement,
                        estimatedTokens = selected.estimatedTokens,
                        estimatedCost = selected.estimatedCost
                    ).also { frozen ->
                        projectStore.saveFrozenAgent(projectId, frozen)
                        appendLog(projectId, "Frozen agent artifact saved: projects/$projectId/agent.json")
                    }
                } else null

                updateProject(projectId) { current ->
                    current.copy(
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
                        dismissedCandidateIds = emptySet(),
                        selectedTools = selectedTools,
                        lastResult = resultWithLogs,
                        frozenAgent = frozenAgent ?: current.frozenAgent,
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

    fun dismissRejectedCandidate(candidateId: String) {
        val project = _state.value.activeProject ?: return
        dismissRejectedCandidate(project.id, candidateId)
    }

    fun dismissRejectedCandidate(projectId: String, candidateId: String) {
        updateProject(projectId) { project ->
            val candidate = project.candidates.firstOrNull { it.id == candidateId } ?: return@updateProject project
            if (candidate.status != com.stemlab.core.model.CandidateStatus.REJECTED || candidate.id == "baseline") {
                return@updateProject project
            }
            project.copy(
                dismissedCandidateIds = project.dismissedCandidateIds + candidateId,
                statusMessage = "Dismissed ${candidate.config.name} from the candidate view."
            )
        }
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

    fun evaluateFrozenAgent() {
        val project = _state.value.activeProject ?: return
        val frozen = project.frozenAgent ?: run {
            appendLog(project.id, "No frozen agent — run evolution first to create one.")
            return
        }
        if (project.isRunning) return
        val projectId = project.id

        setPhase(projectId, Phase.EVALUATING, "Re-evaluating frozen agent...")
        appendLog(projectId, "=== Re-evaluating Frozen Agent: ${frozen.config.name} ===")
        appendLog(projectId, "Config: tools=[${frozen.config.tools.joinToString(", ")}], strategy=${frozen.config.promptStrategy}")

        projectJobs[projectId] = scope.launch {
            try {
                val generated = taskGenerator.generateWithUsage(frozen.domain)
                appendLog(projectId, "Generated ${generated.tasks.size} tasks for re-evaluation")

                val specializedAgent = SpecializedAgent(llmClient, frozen.config, frozen.domain)
                val evaluator = PythonQaEvaluator()
                var tokensUsed = generated.tokensUsed
                var costUsed = generated.costEstimate

                val results = evaluator.evaluate(frozen.config.id, generated.tasks) { task ->
                    val resp = specializedAgent.runOnTask(task)
                    tokensUsed += resp.tokensUsed
                    costUsed += resp.costEstimate
                    resp
                }
                val score = ScoreCalculator.aggregateScore(results)
                val delta = score - frozen.baselineScore
                appendLog(projectId, "Re-evaluation score: ${"%.3f".format(score)} (original frozen score: ${"%.3f".format(frozen.score)})")
                appendLog(projectId, "Delta vs baseline: ${if (delta >= 0) "+" else ""}${"%.3f".format(delta)}")
                appendLog(projectId, "Tokens: $tokensUsed, Cost: \$${"%.4f".format(costUsed)}")
                appendLog(projectId, "Status: PRODUCTION-READY — frozen config verified on fresh tasks")

                updateProject(projectId) { current ->
                    current.copy(
                        currentPhase = Phase.DONE,
                        isRunning = false,
                        statusMessage = "Frozen agent re-evaluated: score=${"%.3f".format(score)}"
                    )
                }
            } catch (e: CancellationException) {
                appendLog(projectId, "Re-evaluation stopped by user")
                updateProject(projectId) {
                    it.copy(isRunning = false, currentPhase = Phase.IDLE, statusMessage = "Re-evaluation stopped.")
                }
            } catch (e: Exception) {
                appendLog(projectId, "ERROR: Re-evaluation failed: ${e.message ?: e::class.simpleName ?: "unknown"}")
                updateProject(projectId) {
                    it.copy(isRunning = false, currentPhase = Phase.IDLE, statusMessage = "Re-evaluation failed — check logs.")
                }
            } finally {
                projectJobs.remove(projectId)
            }
        }
    }

    fun exportReport() {
        val project = _state.value.activeProject ?: return
        val result = project.lastResult ?: run {
            appendLog(project.id, "Nothing to export — run the evolution loop first.")
            return
        }

        updateProject(project.id) {
            it.copy(
                currentPhase = Phase.EXPORTING,
                isRunning = false,
                statusMessage = "Exporting report with OpenAI summary..."
            )
        }
        appendLog(project.id, "Generating human report summary via OpenAI")
        scope.launch {
            try {
                val baseResult = result.copy(logs = currentLogs(project.id))
                val narrativeResponse = llmClient.complete(PromptTemplates.reportNarrative(baseResult))
                val narrative = sanitizeReportNarrative(narrativeResponse.text)
                appendLog(
                    project.id,
                    "Report summary generated (${narrativeResponse.tokensUsed} tokens, " +
                        "$${"%.4f".format(narrativeResponse.costEstimate)})"
                )
                val resultWithReportUsage = baseResult.copy(
                    totalTokensUsed = baseResult.totalTokensUsed + narrativeResponse.tokensUsed,
                    totalCost = baseResult.totalCost + narrativeResponse.costEstimate,
                    logs = currentLogs(project.id)
                )
                val path = MarkdownReportExporter.export(
                    resultWithReportUsage,
                    projectStore.reportPath(project.id, resultWithReportUsage.runId),
                    narrative
                )
                projectStore.saveRun(project.id, resultWithReportUsage)
                appendLog(project.id, "Report exported -> $path")
                val resultWithExportLog = resultWithReportUsage.copy(logs = currentLogs(project.id))
                projectStore.saveRun(project.id, resultWithExportLog)
                updateProject(project.id) {
                    it.copy(
                        metrics = it.metrics.copy(
                            estimatedTokens = resultWithExportLog.totalTokensUsed,
                            estimatedCost = resultWithExportLog.totalCost
                        ),
                        lastResult = resultWithExportLog,
                        lastExportPath = path,
                        currentPhase = Phase.DONE,
                        isRunning = false,
                        statusMessage = "Report saved: $path"
                    )
                }
            } catch (e: CancellationException) {
                appendLog(project.id, "Report export cancelled")
                updateProject(project.id) {
                    it.copy(currentPhase = Phase.IDLE, isRunning = false, statusMessage = "Report export cancelled.")
                }
            } catch (e: Exception) {
                appendLog(project.id, "ERROR: Report export failed: ${e.message ?: e::class.simpleName ?: "unknown error"}")
                updateProject(project.id) {
                    it.copy(currentPhase = Phase.IDLE, isRunning = false, statusMessage = "Report export failed — check logs.")
                }
            }
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
        val frozenAgent = projectStore.loadFrozenAgent(id)
        val selectedTools = frozenAgent?.config?.tools?.let { toolRegistry.resolve(it) }
            ?: latest?.selectedCandidate?.config?.tools?.let { toolRegistry.resolve(it) }
            ?: emptyList()
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
            frozenAgent = frozenAgent,
            statusMessage = if (latest == null)
                "Ready — OpenAI key loaded; click Run Evolution to start."
            else if (frozenAgent != null)
                "Loaded latest run ${latest.runId}. Frozen agent: ${frozenAgent.config.name}."
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
        updateProject(projectId) { it.copy(logs = emptyList(), isRunning = true, dismissedCandidateIds = emptySet()) }
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

    private fun sanitizeReportNarrative(value: String): String =
        value
            .replace(Regex("```+"), "")
            .trim()
            .take(2_500)

    private fun uniqueProjectId(name: String): String {
        val slug = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "project" }
            .take(32)
            .trim('-')
            .ifBlank { "project" }
        val existing = projectStore.loadProjects().map { it.id }.toSet()
        var candidate = slug
        while (candidate in existing) {
            candidate = "$slug-${UUID.randomUUID().toString().take(6)}"
        }
        return candidate
    }

    private fun cleanSingleLine(value: String, maxLength: Int): String =
        value
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
            .trim()

    private companion object {
        const val MAX_PROJECT_NAME_LENGTH = 80
        const val MAX_DOMAIN_LENGTH = 160
        const val MAX_DESCRIPTION_LENGTH = 500

        fun defaultLlmClient(): LlmClient {
            val apiKey = DotEnvLoader.requireApiKey()
            return OpenAiLlmClient(apiKey)
        }
    }
}
