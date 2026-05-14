package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CandidateConfigParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ProposalResponse(val candidates: List<AgentConfig>)

    fun parse(llmResponse: String): List<AgentConfig>? = try {
        val jsonText = extractJsonObject(llmResponse)
        val parsed = json.decodeFromString<ProposalResponse>(jsonText)
        parsed.candidates.takeIf { it.size == 3 }
    } catch (e: Exception) {
        null
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) throw IllegalArgumentException("No JSON object in response")
        return text.substring(start, end + 1)
    }
}
