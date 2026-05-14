package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates

class StemAgent(private val llmClient: LlmClient) {

    suspend fun proposeCandidates(domain: String, baselineScore: Double): List<AgentConfig> {
        val response = llmClient.complete(PromptTemplates.candidateProposal(domain, baselineScore))

        return CandidateConfigParser.parse(response.text)
            ?: error("OpenAI did not return exactly 3 valid candidate configurations.")
    }
}
