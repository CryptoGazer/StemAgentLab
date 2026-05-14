package com.stemlab.core.tasks

import com.stemlab.core.model.EvalTask
import com.stemlab.llm.LlmClient
import com.stemlab.llm.PromptTemplates
import com.stemlab.util.JsonExtractor
import kotlinx.serialization.json.Json

data class GeneratedTasks(
    val tasks: List<EvalTask>,
    val tokensUsed: Int,
    val costEstimate: Double
)

class LlmTaskGenerator(
    private val llmClient: LlmClient
) : TaskGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(domain: String, count: Int): List<EvalTask> {
        return generateWithUsage(domain, count).tasks
    }

    suspend fun generateWithUsage(domain: String, count: Int = 5): GeneratedTasks {
        var lastException: Exception? = null
        for (attempt in 1..2) {
            try {
                val response = llmClient.complete(PromptTemplates.generateTasks(domain, count))
                val raw = sanitizeCodeFields(response.text)
                val jsonText = JsonExtractor.extractArray(raw)
                val tasks = json.decodeFromString<List<EvalTask>>(jsonText).take(count)
                require(tasks.isNotEmpty()) { "OpenAI returned no evaluation tasks." }
                return GeneratedTasks(
                    tasks = tasks,
                    tokensUsed = response.tokensUsed,
                    costEstimate = response.costEstimate
                )
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw IllegalStateException(
            "Failed to generate evaluation tasks with OpenAI: ${lastException?.message}",
            lastException
        )
    }

    // Replace literal (non-escaped) newlines inside JSON string values with \n,
    // which is the most common cause of parse failures when LLMs embed multi-line code.
    private fun sanitizeCodeFields(text: String): String {
        val sb = StringBuilder()
        var inString = false
        var escape = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                escape -> {
                    sb.append(c)
                    escape = false
                }
                c == '\\' && inString -> {
                    sb.append(c)
                    escape = true
                }
                c == '"' -> {
                    inString = !inString
                    sb.append(c)
                }
                inString && c == '\n' -> sb.append("\\n")
                inString && c == '\r' -> sb.append("\\r")
                inString && c == '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}
