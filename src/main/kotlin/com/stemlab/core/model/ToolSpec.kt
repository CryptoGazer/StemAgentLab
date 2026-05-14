package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ToolSpec(
    val id: String,
    val name: String,
    val description: String,
    val estimatedTokensPerCall: Int,
    val estimatedCostPerCall: Double
)
