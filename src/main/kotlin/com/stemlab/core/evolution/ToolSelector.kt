package com.stemlab.core.evolution

import com.stemlab.core.registry.ToolRegistry

class ToolSelector(private val registry: ToolRegistry) {

    fun selectForBaseline(): List<String> = emptyList()

    fun selectForCandidate(tier: Int): List<String> = when (tier) {
        1 -> listOf("code_reader", "static_analyzer")
        2 -> listOf("code_reader", "test_generator", "python_runner", "failure_analyzer")
        3 -> listOf("code_reader", "test_generator", "python_runner", "patch_suggester", "failure_analyzer")
        else -> emptyList()
    }

    fun estimateCost(toolIds: List<String>): Double = registry.estimatedCost(toolIds)

    fun estimateTokens(toolIds: List<String>): Int = registry.estimatedTokens(toolIds)
}
