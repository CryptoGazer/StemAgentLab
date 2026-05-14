package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.LlmResponse
import com.stemlab.llm.PromptTemplates

class BaselineAgent(private val llmClient: LlmClient, private val domain: String) {

    val config = AgentConfig(
        id = "baseline",
        name = "Baseline Agent",
        tools = emptyList(),
        skills = listOf("direct_reasoning"),
        promptStrategy = "direct",
        maxTokens = 500
    )

    suspend fun runOnTask(task: EvalTask): LlmResponse {
        val prompt = PromptTemplates.baseline(task, domain)
        return llmClient.complete(prompt)
    }
}
