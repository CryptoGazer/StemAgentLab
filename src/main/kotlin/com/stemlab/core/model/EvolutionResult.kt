package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EvolutionResult(
    val runId: String,
    val domain: String,
    val baselineScore: Double,
    val candidates: List<CandidateAgent>,
    val selectedCandidateId: String?,
    val improvement: Double,
    val totalTokensUsed: Long,
    val totalCost: Double,
    val logs: List<String>,
    val timestamp: String
) {
    val selectedCandidate: CandidateAgent?
        get() = candidates.find { it.id == selectedCandidateId }

    val improvementPercent: Double
        get() = if (baselineScore > 0) (improvement / baselineScore) * 100.0 else 0.0
}
