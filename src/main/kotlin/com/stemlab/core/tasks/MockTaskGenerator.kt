package com.stemlab.core.tasks

import com.stemlab.app.DemoScenario
import com.stemlab.core.model.EvalTask

class MockTaskGenerator : TaskGenerator {

    override suspend fun generate(domain: String, count: Int): List<EvalTask> {
        val lower = domain.lowercase()
        return if (lower.contains("python") || lower.contains("qa") || lower.contains("code")) {
            DemoScenario.loadTasks().take(count)
        } else {
            genericTasksFor(domain).take(count)
        }
    }

    private fun genericTasksFor(domain: String): List<EvalTask> = listOf(
        EvalTask(
            id = "gen_001",
            description = "Identify edge case handling issues in $domain context",
            code = "def process(data):\n    return data['value'] * 2",
            expectedIssueKeywords = listOf("edge case", "None check", "KeyError")
        ),
        EvalTask(
            id = "gen_002",
            description = "Detect missing boundary validation for $domain input",
            code = "def validate(n):\n    return n > 0",
            expectedIssueKeywords = listOf("boundary", "zero", "negative")
        ),
        EvalTask(
            id = "gen_003",
            description = "Find type coercion issues in $domain pipeline",
            code = "def combine(a, b):\n    return a + b",
            expectedIssueKeywords = listOf("type mismatch", "string", "integer")
        ),
        EvalTask(
            id = "gen_004",
            description = "Spot resource leak in $domain module",
            code = "def read_file(path):\n    f = open(path)\n    return f.read()",
            expectedIssueKeywords = listOf("resource leak", "close", "context manager")
        ),
        EvalTask(
            id = "gen_005",
            description = "Identify off-by-one error in $domain loop",
            code = "def last(items):\n    return items[len(items)]",
            expectedIssueKeywords = listOf("off-by-one", "index", "IndexError")
        )
    )
}
