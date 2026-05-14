package com.stemlab.core.tasks

import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates
import com.stemlab.util.JsonExtractor
import kotlinx.serialization.json.Json

class LlmTaskGenerator(
    private val llmClient: LlmClient,
    private val fallback: TaskGenerator = MockTaskGenerator()
) : TaskGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(domain: String, count: Int): List<EvalTask> {
        if (llmClient.isMock) return fallback.generate(domain, count)
        return try {
            val response = llmClient.complete(PromptTemplates.generateTasks(domain, count))
            val jsonText = JsonExtractor.extractArray(response.text)
            json.decodeFromString<List<EvalTask>>(jsonText).take(count)
        } catch (e: Exception) {
            fallback.generate(domain, count)
        }
    }
}
