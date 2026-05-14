package com.stemlab.core.tasks

import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates
import com.stemlab.util.JsonExtractor
import kotlinx.serialization.json.Json

class LlmTaskGenerator(
    private val llmClient: LlmClient
) : TaskGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(domain: String, count: Int): List<EvalTask> {
        return try {
            val response = llmClient.complete(PromptTemplates.generateTasks(domain, count))
            val jsonText = JsonExtractor.extractArray(response.text)
            val tasks = json.decodeFromString<List<EvalTask>>(jsonText).take(count)
            require(tasks.isNotEmpty()) { "OpenAI returned no evaluation tasks." }
            tasks
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate evaluation tasks with OpenAI: ${e.message}", e)
        }
    }
}
