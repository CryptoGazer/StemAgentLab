package com.stemlab.core.tasks

import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates
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
            val jsonText = extractJsonArray(response.text)
            json.decodeFromString<List<EvalTask>>(jsonText).take(count)
        } catch (e: Exception) {
            fallback.generate(domain, count)
        }
    }

    private fun extractJsonArray(text: String): String {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) throw IllegalArgumentException("No JSON array in response")
        return text.substring(start, end + 1)
    }
}
