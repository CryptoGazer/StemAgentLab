package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.util.JsonExtractor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CandidateConfigParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ProposalResponse(val candidates: List<AgentConfig>)

    fun parse(llmResponse: String): List<AgentConfig>? = try {
        val jsonText = JsonExtractor.extractObject(llmResponse)
        val parsed = json.decodeFromString<ProposalResponse>(jsonText)
        parsed.candidates.takeIf { it.size == 3 }
    } catch (e: Exception) {
        null
    }
}
