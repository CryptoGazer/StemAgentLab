package com.stemlab.llm

import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MockLlmClient : LlmClient {

    override val isMock: Boolean = true

    // Task-specific full keyword sets that an ideal agent would mention
    private val taskKeywords = mapOf(
        "task_001" to listOf(
            "ZeroDivisionError", "division by zero", "check b != 0",
            "zero denominator", "raise ValueError", "guard clause", "b == 0"
        ),
        "task_002" to listOf(
            "IndexError", "off-by-one", "index out of range",
            "len(lst) - 1", "last element", "zero-indexed", "lst[-1]"
        ),
        "task_003" to listOf(
            "mutable default argument", "shared state", "None default",
            "to=None", "if to is None", "evaluated once", "side effect"
        ),
        "task_004" to listOf(
            "type mismatch", "string vs integer", "int(user_input)",
            "isinstance", "type coercion", "strict equality", "type conversion"
        ),
        "task_005" to listOf(
            "None check", "KeyError", "AttributeError",
            "missing key", "data.get('name')", "NoneType", "defensive check"
        )
    )

    override suspend fun complete(prompt: String): LlmResponse {
        // Simulate network latency
        delay(200L + (50..300L).random())

        val taskId = extractField(prompt, "TASK_ID")
        val toolIds = extractToolIds(prompt)
        val coverage = coverageFor(toolIds)

        val keywords = taskKeywords[taskId] ?: listOf("potential issue", "code review needed")
        val matchCount = (keywords.size * coverage).roundToInt().coerceIn(1, keywords.size)
        val matched = keywords.take(matchCount)

        val response = buildResponse(taskId, matched, toolIds, coverage)
        val tokens = 180 + toolIds.size * 190
        val cost = tokens * 0.000002

        return LlmResponse(response, tokens, cost)
    }

    private fun coverageFor(toolIds: List<String>): Double = when {
        toolIds.containsAll(listOf("code_reader", "test_generator", "python_runner", "failure_analyzer")) -> 0.857
        toolIds.containsAll(listOf("code_reader", "test_generator", "python_runner", "patch_suggester")) -> 0.771
        toolIds.containsAll(listOf("code_reader", "static_analyzer")) -> 0.543
        else -> 0.314
    }

    private fun buildResponse(
        taskId: String?,
        matchedKeywords: List<String>,
        toolIds: List<String>,
        coverage: Double
    ): String {
        val preamble = if (toolIds.isEmpty()) {
            "Direct analysis (no tools):"
        } else {
            "Analysis using ${toolIds.joinToString(", ")}:"
        }

        val findings = matchedKeywords.mapIndexed { i, kw ->
            "  ${i + 1}. Identified: $kw"
        }.joinToString("\n")

        val confidence = when {
            coverage >= 0.8 -> "HIGH"
            coverage >= 0.5 -> "MEDIUM"
            else -> "LOW"
        }

        return """
$preamble

Task: $taskId
Confidence: $confidence (coverage ${(coverage * 100).roundToInt()}%)

Findings:
$findings

${if (toolIds.isEmpty()) "Recommendation: Manual review suggested — limited tooling available." else "All findings verified via tool outputs."}
        """.trimIndent()
    }

    private fun extractField(prompt: String, field: String): String? =
        prompt.lines()
            .firstOrNull { it.startsWith("$field:") }
            ?.removePrefix("$field:")
            ?.trim()

    private fun extractToolIds(prompt: String): List<String> {
        val line = prompt.lines().firstOrNull { it.startsWith("TOOLS:") } ?: return emptyList()
        val value = line.removePrefix("TOOLS:").trim()
        if (value == "(none)" || value.isBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

}
