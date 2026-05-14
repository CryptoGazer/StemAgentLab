package com.stemlab.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentConfig(
    val id: String,
    val name: String,
    val description: String = "",
    val tools: List<String>,
    val skills: List<String>,
    val promptStrategy: String = "direct",
    val maxTokens: Int = 1000
)
