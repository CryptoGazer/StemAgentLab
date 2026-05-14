package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FrozenAgent(
    val projectId: String,
    val runId: String,
    val domain: String,
    val frozenAt: String,
    val config: AgentConfig,
    val score: Double,
    val baselineScore: Double,
    val improvement: Double,
    val estimatedTokens: Long,
    val estimatedCost: Double
) {
    val improvementPercent: Double
        get() = if (baselineScore > 0) (improvement / baselineScore) * 100.0 else 0.0
}
