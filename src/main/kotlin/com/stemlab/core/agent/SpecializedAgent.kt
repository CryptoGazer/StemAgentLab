package com.stemlab.core.agent

import com.stemlab.core.model.AgentConfig
import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.LlmResponse
import com.stemlab.llm.PromptTemplates

class SpecializedAgent(
    private val llmClient: LlmClient,
    val config: AgentConfig,
    private val domain: String
) {
    suspend fun runOnTask(task: EvalTask): LlmResponse {
        val prompt = PromptTemplates.toolAugmented(task, config, domain)
        return llmClient.complete(prompt)
    }
}
