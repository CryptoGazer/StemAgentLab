package com.stemlab.core.registry

import com.stemlab.core.model.ToolSpec
import kotlinx.serialization.json.Json

class ToolRegistry {

    private val tools: Map<String, ToolSpec> by lazy { loadTools() }

    fun all(): List<ToolSpec> = tools.values.toList()

    fun find(id: String): ToolSpec? = tools[id]

    fun resolve(ids: List<String>): List<ToolSpec> = ids.map {
        tools[it] ?: genericToolSpec(it)
    }

    private fun genericToolSpec(id: String) = com.stemlab.core.model.ToolSpec(
        id = id,
        name = id.replace("_", " ").replaceFirstChar { it.uppercase() },
        description = "Generic tool: $id",
        estimatedTokensPerCall = 200,
        estimatedCostPerCall = 0.0004
    )

    fun estimatedTokens(ids: List<String>): Int =
        resolve(ids).sumOf { it.estimatedTokensPerCall }

    fun estimatedCost(ids: List<String>): Double =
        resolve(ids).sumOf { it.estimatedCostPerCall }

    private fun loadTools(): Map<String, ToolSpec> {
        val json = Json { ignoreUnknownKeys = true }
        val resource = ToolRegistry::class.java.getResourceAsStream("/registries/tools.json")
            ?: error("tools.json not found in resources")
        val list = json.decodeFromString<List<ToolSpec>>(resource.bufferedReader().readText())
        return list.associateBy { it.id }
    }
}
