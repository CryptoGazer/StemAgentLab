package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CandidateStatus { PENDING, EVALUATING, SELECTED, REJECTED }

@Serializable
data class CandidateAgent(
    val id: String,
    val config: AgentConfig,
    val score: Double = 0.0,
    val estimatedCost: Double = 0.0,
    val estimatedTokens: Long = 0L,
    val status: CandidateStatus = CandidateStatus.PENDING
) {
    // score² / cost amplifies high-scoring agents — a 2× better score is 4× more attractive
    val scoreCostRatio: Double
        get() = if (estimatedCost > 0) (score * score) / estimatedCost else 0.0
}
