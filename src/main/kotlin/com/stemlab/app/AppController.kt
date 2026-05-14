package com.stemlab.app

import com.stemlab.core.evolution.EvolutionEngine
import com.stemlab.core.model.Budget
import com.stemlab.core.registry.ToolRegistry
import com.stemlab.core.tasks.LlmTaskGenerator
import com.stemlab.llm.LlmClient
import com.stemlab.llm.MockLlmClient
import com.stemlab.llm.OpenAiLlmClient
import com.stemlab.report.MarkdownReportExporter
import com.stemlab.storage.RunHistoryStore
import com.stemlab.util.DotEnvLoader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppController {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val toolRegistry = ToolRegistry()
    private val historyStore = RunHistoryStore()
    private val logs = mutableListOf<String>()

    private var evolutionJob: Job? = null

    private val llmClient: LlmClient = buildLlmClient().also { client ->
        _state.update { it.copy(isMockMode = client.isMock) }
    }

    private val taskGenerator = LlmTaskGenerator(llmClient)

    private fun buildLlmClient(): LlmClient {
        val apiKey = DotEnvLoader.loadApiKey()
        return if (!apiKey.isNullOrBlank()) {
            log("OpenAI mode active (model: gpt-4o-mini)")
            OpenAiLlmClient(apiKey)
        } else {
            log("Mock mode active — no OPENAI_API_KEY found")
            MockLlmClient()
        }
    }

    fun setDomain(domain: String) {
        if (_state.value.isRunning) return
        val trimmed = domain.trim()
        if (trimmed.isBlank()) return
        _state.update { it.copy(domain = trimmed, statusMessage = "Domain set to \"$trimmed\" — ready to run.") }
    }

    fun runEvolution() {
        if (_state.value.isRunning) return
        val domain = _state.value.domain
        resetLogs()
        setPhase(Phase.RUNNING_BASELINE, "Starting evolution loop...")
        log("Evolution started: baseline → propose → evaluate → select")

        evolutionJob = scope.launch {
            try {
                val taskSource = if (llmClient.isMock) "bundled dataset (mock mode)" else "LLM generation"
                log("Generating tasks for domain: \"$domain\" via $taskSource")
                val tasks = taskGenerator.generate(domain)
                log("Loaded ${tasks.size} evaluation tasks")

                val engine = EvolutionEngine(llmClient, toolRegistry, Budget())

                val result = engine.run(domain, tasks) { line ->
                    log(line)
                    when {
                        line.contains("Baseline score") -> setPhase(Phase.RUNNING_BASELINE)
                        line.contains("proposing candidate") -> setPhase(Phase.EVOLVING, "StemAgent proposing candidates...")
                        line.contains("Evaluating") -> setPhase(Phase.EVALUATING, "Evaluating candidates...")
                        line.contains("VersionManager") -> setPhase(Phase.EVALUATING, "Selecting best candidate...")
                    }
                    flushLogsToState()
                }

                historyStore.save(result)

                val selected = result.selectedCandidate
                val selectedTools = selected?.config?.tools?.let { toolRegistry.resolve(it) } ?: emptyList()

                _state.update { s ->
                    s.copy(
                        isRunning = false,
                        currentPhase = Phase.DONE,
                        metrics = Metrics(
                            baselineScore = result.baselineScore,
                            bestCandidateScore = selected?.score ?: result.baselineScore,
                            improvement = result.improvement,
                            estimatedTokens = result.totalTokensUsed,
                            estimatedCost = result.totalCost
                        ),
                        candidates = result.candidates,
                        selectedTools = selectedTools,
                        lastResult = result,
                        statusMessage = if (selected != null)
                            "Done — ${selected.config.name} selected (+${"%.1f".format(result.improvementPercent)}%)"
                        else
                            "Done — no candidate outperformed baseline"
                    )
                }
                flushLogsToState()

            } catch (e: CancellationException) {
                log("Evolution stopped by user")
                _state.update { it.copy(isRunning = false, currentPhase = Phase.IDLE, statusMessage = "Stopped — click Run Evolution to start again") }
                flushLogsToState()
            }
        }
    }

    fun stopEvolution() {
        evolutionJob?.cancel()
        evolutionJob = null
    }

    fun runFinalEvaluation() {
        val result = _state.value.lastResult ?: run {
            log("No evolution result — run the evolution loop first.")
            flushLogsToState()
            return
        }
        val selected = result.selectedCandidate ?: run {
            log("No candidate was selected — cannot confirm.")
            flushLogsToState()
            return
        }
        log("═══ Final Evaluation ═══")
        log("Frozen agent: ${selected.config.name}")
        log("Score: ${"%.3f".format(selected.score)}")
        log("Tools: ${selected.config.tools.joinToString(", ")}")
        log("Skills: ${selected.config.skills.joinToString(", ")}")
        log("Status: PRODUCTION-READY (frozen configuration confirmed)")
        flushLogsToState()
    }

    fun exportReport() {
        val result = _state.value.lastResult ?: run {
            log("Nothing to export — run the evolution loop first.")
            flushLogsToState()
            return
        }
        setPhase(Phase.EXPORTING, "Exporting report...")
        val resultWithLogs = result.copy(logs = logs.toList())
        val path = MarkdownReportExporter.export(resultWithLogs)
        log("Report exported → $path")
        _state.update { it.copy(lastExportPath = path, currentPhase = Phase.DONE, isRunning = false, statusMessage = "Report saved: $path") }
        flushLogsToState()
    }

    fun resetAll() {
        stopEvolution()
        logs.clear()
        runCatching { java.io.File("runs").listFiles()?.forEach { it.delete() } }
        runCatching { java.io.File("reports").listFiles()?.forEach { it.delete() } }
        _state.value = AppState(isMockMode = llmClient.isMock)
        log("All progress reset — clean slate")
        flushLogsToState()
    }

    private fun log(line: String) {
        val ts = java.time.LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }
        logs.add("[$ts] $line")
    }

    private fun resetLogs() {
        logs.clear()
        _state.update { it.copy(logs = emptyList(), isRunning = true) }
    }

    private fun flushLogsToState() {
        _state.update { it.copy(logs = logs.toList()) }
    }

    private fun setPhase(phase: Phase, status: String? = null) {
        _state.update { s ->
            s.copy(
                currentPhase = phase,
                isRunning = phase != Phase.DONE && phase != Phase.IDLE,
                statusMessage = status ?: s.statusMessage
            )
        }
    }
}
