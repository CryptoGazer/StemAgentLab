package com.stemlab.app

import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.EvolutionResult
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

data class AppState(
    val domain: String = "Python QA",
    val isRunning: Boolean = false,
    val currentPhase: Phase = Phase.IDLE,
    val metrics: Metrics = Metrics(),
    val candidates: List<CandidateAgent> = emptyList(),
    val selectedTools: List<ToolSpec> = emptyList(),
    val logs: List<String> = emptyList(),
    val lastResult: EvolutionResult? = null,
    val lastExportPath: String? = null,
    val statusMessage: String = "Ready — OpenAI key loaded; click Run Baseline to start the evolution loop."
)
