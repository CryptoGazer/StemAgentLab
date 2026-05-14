package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.core.model.CandidateAgent
import com.stemlab.core.model.CandidateStatus
import com.stemlab.llm.LlmClient

class CandidateAgentBuilder(private val llmClient: LlmClient, private val domain: String) {

    fun build(config: AgentConfig): Pair<CandidateAgent, SpecializedAgent> {
        val candidate = CandidateAgent(
            id = config.id,
            config = config,
            status = CandidateStatus.PENDING
        )
        val agent = SpecializedAgent(llmClient, config, domain)
        return candidate to agent
    }

    fun buildAll(configs: List<AgentConfig>): List<Pair<CandidateAgent, SpecializedAgent>> =
        configs.map { build(it) }
}
