package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EvalResult(
    val taskId: String,
    val agentId: String,
    val agentResponse: String,
    val matchedKeywords: List<String>,
    val score: Double,
    val tokensUsed: Int,
    val costEstimate: Double
)
