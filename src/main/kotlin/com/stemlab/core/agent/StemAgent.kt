package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates

class StemAgent(private val llmClient: LlmClient) {

    suspend fun proposeCandidates(domain: String, baselineScore: Double): List<AgentConfig> {
        val response = llmClient.complete(PromptTemplates.candidateProposal(domain, baselineScore))

        if (!llmClient.isMock) {
            CandidateConfigParser.parse(response.text)?.let { return it }
        }

        return defaultCandidatesFor(domain)
    }

    private fun defaultCandidatesFor(domain: String): List<AgentConfig> = listOf(
        AgentConfig(
            id = "candidate_a",
            name = "Candidate A",
            description = "Lightweight static analysis for $domain",
            tools = listOf("code_reader", "static_analyzer"),
            skills = listOf("direct_reasoning", "edge_case_reasoning"),
            promptStrategy = "tool_augmented",
            maxTokens = 800
        ),
        AgentConfig(
            id = "candidate_b",
            name = "Candidate B",
            description = "Full evaluation pipeline for $domain",
            tools = listOf("code_reader", "test_generator", "python_runner", "failure_analyzer"),
            skills = listOf("direct_reasoning", "edge_case_reasoning", "test_driven_analysis", "root_cause_analysis"),
            promptStrategy = "tool_augmented",
            maxTokens = 1200
        ),
        AgentConfig(
            id = "candidate_c",
            name = "Candidate C",
            description = "Maximal tooling with patch suggestion for $domain",
            tools = listOf("code_reader", "test_generator", "python_runner", "patch_suggester", "failure_analyzer"),
            skills = listOf("direct_reasoning", "edge_case_reasoning", "test_driven_analysis", "root_cause_analysis", "patch_suggestion"),
            promptStrategy = "tool_augmented",
            maxTokens = 1500
        )
    )
}
