package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.llm.LlmClient
import com.stemlab.llm.LlmResponse
import com.stemlab.llm.PromptTemplates

data class CandidateProposal(
    val configs: List<AgentConfig>,
    val tokensUsed: Int,
    val costEstimate: Double
)

class StemAgent(private val llmClient: LlmClient) {

    suspend fun proposeCandidates(domain: String, baselineScore: Double): List<AgentConfig> {
        return proposeCandidateSet(domain, baselineScore).configs
    }

    suspend fun proposeCandidateSet(domain: String, baselineScore: Double): CandidateProposal {
        val response = llmClient.complete(PromptTemplates.candidateProposal(domain, baselineScore))
        val configs = parseConfigs(response)

        return CandidateProposal(
            configs = configs,
            tokensUsed = response.tokensUsed,
            costEstimate = response.costEstimate
        )
    }

    private fun parseConfigs(response: LlmResponse): List<AgentConfig> =
        CandidateConfigParser.parse(response.text)
            ?: error("OpenAI did not return exactly 3 valid candidate configurations.")
}
