package com.stemlab.app

import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.EvolutionResult
import com.stemlab.core.model.ProjectSpec
import com.stemlab.core.model.ToolSpec

enum class Phase { IDLE, RUNNING_BASELINE, EVOLVING, EVALUATING, DONE, EXPORTING }

data class Metrics(
    val baselineScore: Double = 0.0,
    val bestCandidateScore: Double = 0.0,
    val improvement: Double = 0.0,
    val estimatedTokens: Long = 0L,
    val estimatedCost: Double = 0.0
) {
    val improvementPercent: Double
        get() = if (baselineScore > 0) (improvement / baselineScore) * 100.0 else 0.0
}

data class ProjectViewState(
    val spec: ProjectSpec,
    val isRunning: Boolean = false,
    val currentPhase: Phase = Phase.IDLE,
    val metrics: Metrics = Metrics(),
    val candidates: List<CandidateAgent> = emptyList(),
    val selectedTools: List<ToolSpec> = emptyList(),
    val logs: List<String> = emptyList(),
    val lastResult: EvolutionResult? = null,
    val lastExportPath: String? = null,
    val statusMessage: String = "Ready — OpenAI key loaded; click Run Evolution to start."
) {
    val id: String get() = spec.id
    val name: String get() = spec.name
    val domain: String get() = spec.domain
}

data class AppState(
    val projects: List<ProjectViewState> = emptyList(),
    val activeProjectId: String? = null,
    val llmLabel: String = "LLM: OpenAI"
) {
    val activeProject: ProjectViewState?
        get() = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()

    val hasRunningProjects: Boolean
        get() = projects.any { it.isRunning }
}
