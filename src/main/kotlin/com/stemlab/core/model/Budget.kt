package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val maxTokens: Long = 100_000,
    val maxCost: Double = 2.0,
    val maxCandidates: Int = 3,
    val maxRounds: Int = 2,
    val maxParallelCandidates: Int = 2
)
